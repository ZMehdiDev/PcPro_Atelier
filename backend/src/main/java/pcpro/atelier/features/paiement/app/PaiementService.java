package pcpro.atelier.features.paiement.app;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.paiement.api.dto.CaisseDto;
import pcpro.atelier.features.paiement.api.dto.PaiementDto;
import pcpro.atelier.features.paiement.api.dto.PaiementRequest;
import pcpro.atelier.features.paiement.domain.Paiement;
import pcpro.atelier.features.paiement.infra.PaiementRepository;
import pcpro.atelier.features.vente.api.dto.VenteDto;
import pcpro.atelier.features.vente.domain.Vente;
import pcpro.atelier.features.vente.infra.VenteRepository;

/**
 * Encaissements : à la clôture d'un dossier (avec le changement de statut) ou en acompte.
 */
@Service
@Transactional
public class PaiementService {

  private static final int MAX_JOURS_CAISSE = 366;

  private final PaiementRepository paiementRepository;
  private final DossierRepository dossierRepository;
  private final VenteRepository venteRepository;
  private final EvenementService evenementService;
  private final CurrentEmploye currentEmploye;
  private final Clock clock;

  public PaiementService(
      PaiementRepository paiementRepository,
      DossierRepository dossierRepository,
      VenteRepository venteRepository,
      EvenementService evenementService,
      CurrentEmploye currentEmploye,
      Clock clock
  ) {
    this.paiementRepository = paiementRepository;
    this.dossierRepository = dossierRepository;
    this.venteRepository = venteRepository;
    this.evenementService = evenementService;
    this.currentEmploye = currentEmploye;
    this.clock = clock;
  }

  public PaiementDto encaisser(Long dossierId, PaiementRequest request) {
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    if (dossier.estCloture()) {
      throw new ConflictException("Le dossier " + dossier.getReference() + " est clôturé : rouvrez-le pour encaisser");
    }
    return PaiementDto.of(enregistrer(dossier, request));
  }

  /** Enregistre un paiement sur un dossier déjà chargé (utilisé aussi à la clôture). */
  public Paiement enregistrer(Dossier dossier, PaiementRequest request) {
    Paiement paiement = new Paiement();
    paiement.setDossier(dossier);
    paiement.setMontant(request.montant());
    paiement.setMoyen(request.moyen());
    paiement.setEmploye(currentEmploye.get());
    paiement = paiementRepository.save(paiement);

    evenementService.journaliser(dossier, TypeActionEvenement.PAIEMENT, null,
        formater(paiement.getMontant()) + " (" + paiement.getMoyen() + ")", null);
    return paiement;
  }

  /** Correction d'une erreur de saisie (réservé aux administrateurs, contrôlé par le contrôleur). */
  public void annuler(Long dossierId, Long paiementId) {
    Paiement paiement = paiementRepository.findById(paiementId)
        .filter(p -> p.getDossier().getId().equals(dossierId))
        .orElseThrow(() -> new ResourceNotFoundException("Paiement", paiementId));
    evenementService.journaliser(paiement.getDossier(), TypeActionEvenement.ANNULATION_PAIEMENT,
        formater(paiement.getMontant()) + " (" + paiement.getMoyen() + ")", null, null);
    paiementRepository.delete(paiement);
  }

  @Transactional(readOnly = true)
  public List<Paiement> duDossier(Dossier dossier) {
    return paiementRepository.findByDossier(dossier);
  }

  /** Encaissements (réparations et ventes) entre deux dates incluses (aujourd'hui par défaut). */
  @Transactional(readOnly = true)
  public CaisseDto caisse(LocalDate du, LocalDate au) {
    Periode periode = Periode.of(du, au, clock);
    LocalDate debut = periode.du();
    LocalDate fin = periode.au();
    List<Paiement> paiements = paiementRepository.findEntre(periode.debut(), periode.fin());
    List<Vente> ventes = venteRepository.findEntre(periode.debut(), periode.fin());

    Map<MoyenPaiement, BigDecimal> parMoyen = new EnumMap<>(MoyenPaiement.class);
    for (MoyenPaiement moyen : MoyenPaiement.values()) {
      parMoyen.put(moyen, BigDecimal.ZERO);
    }
    paiements.forEach(p -> parMoyen.merge(p.getMoyen(), p.getMontant(), BigDecimal::add));
    ventes.forEach(v -> parMoyen.merge(v.getMoyen(), v.total(), BigDecimal::add));
    BigDecimal totalVentes = ventes.stream().map(Vente::total).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CaisseDto(
        debut,
        fin,
        total(paiements).add(totalVentes),
        parMoyen,
        paiements.stream().map(p -> new CaisseDto.Ligne(
            p.getId(),
            p.getDossier().getId(),
            p.getDossier().getReference(),
            p.getDossier().getClient().getNom(),
            p.getMontant(),
            p.getMoyen(),
            p.getEmploye().getPrenom(),
            p.getCreatedAt()
        )).toList(),
        ventes.stream().map(v -> new CaisseDto.LigneVente(
            v.getId(),
            VenteDto.resume(v),
            v.total(),
            v.getMoyen(),
            v.getEmploye().getPrenom(),
            v.getCreatedAt()
        )).toList()
    );
  }

  /**
   * Période en jours entiers (bornes incluses, aujourd'hui par défaut, un an maximum) et ses instants
   * de début et de fin (exclue) dans le fuseau de l'atelier.
   */
  public record Periode(LocalDate du, LocalDate au, OffsetDateTime debut, OffsetDateTime fin) {

    public static Periode of(LocalDate du, LocalDate au, Clock clock) {
      LocalDate aujourdhui = LocalDate.now(clock);
      LocalDate debut = du != null ? du : (au != null ? au : aujourdhui);
      LocalDate fin = au != null ? au : (du != null ? du : aujourdhui);
      if (fin.isBefore(debut)) {
        throw new BadRequestException("La date de fin est avant la date de début");
      }
      if (debut.plusDays(MAX_JOURS_CAISSE).isBefore(fin)) {
        throw new BadRequestException("Période trop longue (un an maximum)");
      }
      ZoneId zone = clock.getZone();
      return new Periode(debut, fin,
          debut.atStartOfDay(zone).toOffsetDateTime(), fin.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
    }
  }

  public static BigDecimal total(List<Paiement> paiements) {
    return paiements.stream().map(Paiement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String formater(BigDecimal montant) {
    return montant.setScale(2).toPlainString().replace('.', ',') + " €";
  }
}

package pcpro.atelier.features.facture.app;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.enums.TypeFacture;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.infra.DevisRepository;
import pcpro.atelier.features.document.Montants;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.facture.api.dto.FactureDto;
import pcpro.atelier.features.facture.api.dto.FactureRequest;
import pcpro.atelier.features.facture.domain.Facture;
import pcpro.atelier.features.facture.domain.FactureLigne;
import pcpro.atelier.features.facture.infra.FactureRepository;
import pcpro.atelier.features.paiement.app.PaiementService;
import pcpro.atelier.features.paiement.infra.PaiementRepository;
import pcpro.atelier.features.vente.domain.Vente;
import pcpro.atelier.features.vente.infra.VenteRepository;

/**
 * Factures des réparations et des ventes au comptoir.
 *
 * Règles (facturation belge) : numérotation continue sans trou, par année ; une facture émise ne se modifie ni
 * ne se supprime ; une erreur se corrige par une note de crédit qui l'annule, puis une nouvelle facture.
 * Une réparation ou une vente n'a qu'une facture valable à la fois : en redemander une renvoie celle qui existe.
 */
@Service
@Transactional
public class FactureService {

  private final FactureRepository factureRepository;
  private final DossierRepository dossierRepository;
  private final VenteRepository venteRepository;
  private final DevisRepository devisRepository;
  private final PaiementRepository paiementRepository;
  private final EvenementService evenementService;
  private final CurrentEmploye currentEmploye;
  private final AtelierProperties atelier;
  private final JdbcClient jdbcClient;
  private final Clock clock;

  public FactureService(
      FactureRepository factureRepository,
      DossierRepository dossierRepository,
      VenteRepository venteRepository,
      DevisRepository devisRepository,
      PaiementRepository paiementRepository,
      EvenementService evenementService,
      CurrentEmploye currentEmploye,
      AtelierProperties atelier,
      JdbcClient jdbcClient,
      Clock clock
  ) {
    this.factureRepository = factureRepository;
    this.dossierRepository = dossierRepository;
    this.venteRepository = venteRepository;
    this.devisRepository = devisRepository;
    this.paiementRepository = paiementRepository;
    this.evenementService = evenementService;
    this.currentEmploye = currentEmploye;
    this.atelier = atelier;
    this.jdbcClient = jdbcClient;
    this.clock = clock;
  }

  /**
   * Facture d'une réparation, d'après le devis accepté (ou, sans devis, une ligne du montant encaissé).
   * Renvoie la facture existante si le dossier en a déjà une valable.
   */
  public FactureDto facturerDossier(Long dossierId, FactureRequest request) {
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    Facture existante = valable(factureRepository.findByDossierId(dossierId));
    if (existante != null) {
      return FactureDto.of(existante, null);
    }

    Facture facture = nouvelle(TypeFacture.FACTURE);
    facture.setDossier(dossier);
    facture.setClientNom(texte(request.clientNom(), dossier.getClient().getNom()));
    facture.setClientEmail(texte(request.clientEmail(), dossier.getClient().getEmail()));
    facture.setClientAdresse(texte(request.clientAdresse(), null));
    facture.setClientTva(texte(request.clientTva(), null));

    Devis devis = devisRepository.findByDossier(dossier)
        .filter(d -> d.getStatut() == StatutDevis.ACCEPTE)
        .orElse(null);
    if (devis != null) {
      devis.getLignes().forEach(l -> facture.ajouterLigne(new FactureLigne(
          l.getLibelle(), Montants.type(l.getType()), l.getQuantite(), l.getPrixUnitaire(), l.total())));
    } else {
      BigDecimal encaisse = PaiementService.total(paiementRepository.findByDossier(dossier));
      if (encaisse.signum() <= 0) {
        throw new BadRequestException(
            "Rien à facturer : faites accepter un devis ou encaissez la réparation, puis réessayez");
      }
      String appareil = Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele());
      facture.ajouterLigne(new FactureLigne("Réparation " + appareil, "Dossier " + dossier.getReference(),
          BigDecimal.ONE, encaisse, encaisse));
    }
    Facture enregistree = enregistrer(facture);
    evenementService.journaliser(dossier, TypeActionEvenement.FACTURE, null,
        "Facture " + enregistree.getNumero() + " : " + Libelles.montant(enregistree.getTotal()), null);
    return FactureDto.of(enregistree, null);
  }

  /** Facture d'une vente au comptoir (le nom du client est obligatoire, la vente n'en a pas). */
  public FactureDto facturerVente(Long venteId, FactureRequest request) {
    Vente vente = venteRepository.findById(venteId).orElseThrow(() -> new ResourceNotFoundException("Vente", venteId));
    Facture existante = valable(factureRepository.findByVenteId(venteId));
    if (existante != null) {
      return FactureDto.of(existante, null);
    }
    if (request.clientNom() == null || request.clientNom().isBlank()) {
      throw new BadRequestException("Indiquez le nom du client à mettre sur la facture");
    }
    Facture facture = nouvelle(TypeFacture.FACTURE);
    facture.setVente(vente);
    facture.setClientNom(request.clientNom().trim());
    facture.setClientEmail(texte(request.clientEmail(), null));
    facture.setClientAdresse(texte(request.clientAdresse(), null));
    facture.setClientTva(texte(request.clientTva(), null));
    vente.getLignes().forEach(l -> facture.ajouterLigne(new FactureLigne(
        l.getArticle(), null, BigDecimal.valueOf(l.getQuantite()), l.getPrixUnitaire(), l.total())));
    return FactureDto.of(enregistrer(facture), null);
  }

  /** Annule une facture par une note de crédit du même montant, en négatif (réservé aux administrateurs). */
  public FactureDto annuler(Long factureId) {
    Facture facture = factureRepository.findWithLignesById(factureId)
        .orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));
    if (facture.estNoteCredit()) {
      throw new ConflictException("Une note de crédit ne s'annule pas");
    }
    if (factureRepository.findByFactureOrigineId(factureId).isPresent()) {
      throw new ConflictException("La facture " + facture.getNumero() + " est déjà annulée");
    }
    Facture note = nouvelle(TypeFacture.NOTE_CREDIT);
    note.setFactureOrigine(facture);
    note.setDossier(facture.getDossier());
    note.setVente(facture.getVente());
    note.setClientNom(facture.getClientNom());
    note.setClientAdresse(facture.getClientAdresse());
    note.setClientTva(facture.getClientTva());
    note.setClientEmail(facture.getClientEmail());
    // Même régime que la facture annulée, même si la configuration a changé depuis
    note.setRegimeTva(facture.getRegimeTva());
    note.setTauxTva(facture.getTauxTva());
    facture.getLignes().forEach(l -> note.ajouterLigne(new FactureLigne(
        l.getLibelle(), l.getDetail(), l.getQuantite(), l.getPrixUnitaire().negate(), l.getTotal().negate())));
    Facture enregistree = enregistrer(note);
    if (facture.getDossier() != null) {
      evenementService.journaliser(facture.getDossier(), TypeActionEvenement.FACTURE,
          "Facture " + facture.getNumero(), "Annulée par la note de crédit " + enregistree.getNumero(), null);
    }
    return FactureDto.of(enregistree, null);
  }

  /** Facture valable d'un dossier, ou null s'il n'en a pas. */
  @Transactional(readOnly = true)
  public FactureDto duDossier(Long dossierId) {
    Facture facture = valable(factureRepository.findByDossierId(dossierId));
    return facture != null ? FactureDto.of(facture, null) : null;
  }

  @Transactional(readOnly = true)
  public FactureDto deLaVente(Long venteId) {
    Facture facture = valable(factureRepository.findByVenteId(venteId));
    return facture != null ? FactureDto.of(facture, null) : null;
  }

  /** Factures et notes de crédit émises entre deux dates incluses (ce mois-ci par défaut). */
  @Transactional(readOnly = true)
  public List<FactureDto> lister(LocalDate du, LocalDate au) {
    LocalDate aujourdhui = LocalDate.now(clock);
    LocalDate debut = du != null ? du : aujourdhui.withDayOfMonth(1);
    LocalDate fin = au != null ? au : aujourdhui;
    if (fin.isBefore(debut)) {
      throw new BadRequestException("La date de fin est avant la date de début");
    }
    List<Facture> factures = factureRepository.findEntre(debut, fin);
    Map<Long, String> annulations = annulations(factures);
    return factures.stream().map(f -> FactureDto.of(f, annulations.get(f.getId()))).toList();
  }

  @Transactional(readOnly = true)
  public FactureDto get(Long id) {
    Facture facture = getOrThrow(id);
    return FactureDto.of(facture, factureRepository.findByFactureOrigineId(id).map(Facture::getNumero).orElse(null));
  }

  @Transactional(readOnly = true)
  public Facture getOrThrow(Long id) {
    return factureRepository.findWithLignesById(id).orElseThrow(() -> new ResourceNotFoundException("Facture", id));
  }

  // ---------- Interne ----------

  private Facture nouvelle(TypeFacture type) {
    Facture facture = new Facture();
    facture.setType(type);
    facture.setDateEmission(LocalDate.now(clock));
    facture.setRegimeTva(atelier.tva().regime());
    facture.setTauxTva(atelier.tva().taux());
    facture.setEmploye(currentEmploye.get());
    return facture;
  }

  /** Attribue le numéro suivant (compteur en base, sans trou ni doublon) et enregistre. */
  private Facture enregistrer(Facture facture) {
    int annee = facture.getDateEmission().getYear();
    int sequence = jdbcClient.sql("""
        INSERT INTO facture_compteur (type, annee, dernier) VALUES (:type, :annee, 1)
        ON CONFLICT (type, annee) DO UPDATE SET dernier = facture_compteur.dernier + 1
        RETURNING dernier
        """)
        .param("type", facture.getType().name())
        .param("annee", annee)
        .query(Integer.class)
        .single();
    facture.setAnnee(annee);
    facture.setSequence(sequence);
    facture.setNumero(numero(facture.getType(), annee, sequence));
    facture.setTotal(facture.getLignes().stream().map(FactureLigne::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    return factureRepository.save(facture);
  }

  /** Ex : "2026-0001" pour une facture, "NC2026-0001" pour une note de crédit. */
  static String numero(TypeFacture type, int annee, int sequence) {
    return (type == TypeFacture.NOTE_CREDIT ? "NC" : "") + annee + "-" + String.format("%04d", sequence);
  }

  /** La facture non annulée parmi celles d'un dossier ou d'une vente (la plus récente d'abord). */
  private Facture valable(List<Facture> factures) {
    Set<Long> annulees = new HashSet<>();
    factures.stream().filter(Facture::estNoteCredit).forEach(n -> annulees.add(n.getFactureOrigine().getId()));
    return factures.stream()
        .filter(f -> !f.estNoteCredit() && !annulees.contains(f.getId()))
        .findFirst()
        .orElse(null);
  }

  /** Numéro de la note de crédit qui annule chaque facture annulée. */
  private Map<Long, String> annulations(List<Facture> factures) {
    List<Long> ids = factures.stream().filter(f -> !f.estNoteCredit()).map(Facture::getId).toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return factureRepository.notesCreditDe(ids).stream()
        .collect(Collectors.toMap(n -> n.getFactureOrigine().getId(), Facture::getNumero, (a, b) -> a));
  }

  private static String texte(String valeur, String parDefaut) {
    return valeur == null || valeur.isBlank() ? parDefaut : valeur.trim();
  }
}

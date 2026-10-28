package pcpro.atelier.features.facture.app;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.features.document.Formats;
import pcpro.atelier.features.document.PdfRenderer;
import pcpro.atelier.features.facture.api.dto.FactureDto;
import pcpro.atelier.features.paiement.app.PaiementService;
import pcpro.atelier.features.paiement.domain.Paiement;
import pcpro.atelier.features.paiement.infra.PaiementRepository;
import pcpro.atelier.features.vente.domain.Vente;
import pcpro.atelier.features.vente.infra.VenteRepository;

/**
 * Journal des recettes pour le comptable : ce qui a été encaissé chaque jour (réparations et ventes, par moyen
 * de paiement), puis la liste des factures et notes de crédit émises sur la période.
 */
@Service
@Transactional(readOnly = true)
public class JournalRecettesService {

  private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

  private final PaiementRepository paiementRepository;
  private final VenteRepository venteRepository;
  private final FactureService factureService;
  private final PdfRenderer renderer;
  private final Clock clock;

  public JournalRecettesService(
      PaiementRepository paiementRepository,
      VenteRepository venteRepository,
      FactureService factureService,
      PdfRenderer renderer,
      Clock clock
  ) {
    this.paiementRepository = paiementRepository;
    this.venteRepository = venteRepository;
    this.factureService = factureService;
    this.renderer = renderer;
    this.clock = clock;
  }

  public record Journal(String nomFichier, byte[] pdf) {
  }

  /** Recettes d'un jour (ou total de la période). */
  public record Jour(LocalDate date, BigDecimal reparations, BigDecimal ventes, Map<MoyenPaiement, BigDecimal> parMoyen,
                     BigDecimal total, int operations) {

    static Jour vide(LocalDate date) {
      Map<MoyenPaiement, BigDecimal> parMoyen = new EnumMap<>(MoyenPaiement.class);
      for (MoyenPaiement moyen : MoyenPaiement.values()) {
        parMoyen.put(moyen, BigDecimal.ZERO);
      }
      return new Jour(date, BigDecimal.ZERO, BigDecimal.ZERO, parMoyen, BigDecimal.ZERO, 0);
    }

    Jour plus(BigDecimal montant, MoyenPaiement moyen, boolean vente) {
      Map<MoyenPaiement, BigDecimal> nouveau = new EnumMap<>(parMoyen);
      nouveau.merge(moyen, montant, BigDecimal::add);
      return new Jour(date, vente ? reparations : reparations.add(montant), vente ? ventes.add(montant) : ventes,
          nouveau, total.add(montant), operations + 1);
    }

    Jour plus(Jour autre) {
      Map<MoyenPaiement, BigDecimal> nouveau = new EnumMap<>(parMoyen);
      autre.parMoyen.forEach((moyen, montant) -> nouveau.merge(moyen, montant, BigDecimal::add));
      return new Jour(date, reparations.add(autre.reparations), ventes.add(autre.ventes), nouveau,
          total.add(autre.total), operations + autre.operations);
    }

    public BigDecimal moyen(String nom) {
      return parMoyen.get(MoyenPaiement.valueOf(nom));
    }
  }

  /** Données imprimées. */
  public record Vue(String periode, LocalDate du, LocalDate au, List<Jour> jours, Jour total,
                    List<FactureDto> factures, BigDecimal totalFactures) {
  }

  public Journal generer(LocalDate du, LocalDate au) {
    LocalDate aujourdhui = LocalDate.now(clock);
    PaiementService.Periode periode = PaiementService.Periode.of(
        du != null ? du : aujourdhui.withDayOfMonth(1), au != null ? au : aujourdhui, clock);

    Map<LocalDate, Jour> parJour = new TreeMap<>();
    for (Paiement paiement : paiementRepository.findEntre(periode.debut(), periode.fin())) {
      LocalDate jour = Formats.jour(paiement.getCreatedAt());
      parJour.put(jour, parJour.getOrDefault(jour, Jour.vide(jour)).plus(paiement.getMontant(), paiement.getMoyen(), false));
    }
    for (Vente vente : venteRepository.findEntre(periode.debut(), periode.fin())) {
      LocalDate jour = Formats.jour(vente.getCreatedAt());
      parJour.put(jour, parJour.getOrDefault(jour, Jour.vide(jour)).plus(vente.total(), vente.getMoyen(), true));
    }
    List<Jour> jours = new ArrayList<>(parJour.values());
    Jour total = jours.stream().reduce(Jour.vide(null), Jour::plus);

    List<FactureDto> factures = factureService.lister(periode.du(), periode.au());
    BigDecimal totalFactures = factures.stream().map(FactureDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);

    String libelle = libelle(periode.du(), periode.au());
    byte[] pdf = renderer.rendre("journal", "Journal des recettes " + libelle,
        Map.of("j", new Vue(libelle, periode.du(), periode.au(), jours, total, factures, totalFactures)));
    return new Journal("journal-recettes-" + periode.du() + "-" + periode.au() + ".pdf", pdf);
  }

  /** "Octobre 2026" pour un mois entier, sinon "01/10/2026 au 15/10/2026". */
  private static String libelle(LocalDate du, LocalDate au) {
    if (du.getDayOfMonth() == 1 && au.equals(du.withDayOfMonth(du.lengthOfMonth()))) {
      return Formats.INSTANCE.majuscule(MOIS.format(du));
    }
    return du.equals(au) ? Formats.INSTANCE.date(du) : Formats.INSTANCE.date(du) + " au " + Formats.INSTANCE.date(au);
  }
}

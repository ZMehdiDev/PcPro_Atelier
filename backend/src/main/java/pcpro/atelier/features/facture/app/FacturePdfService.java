package pcpro.atelier.features.facture.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.document.Formats;
import pcpro.atelier.features.document.Montants;
import pcpro.atelier.features.document.PdfRenderer;
import pcpro.atelier.features.document.QrCodes;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.facture.domain.Facture;
import pcpro.atelier.features.facture.infra.FactureRepository;
import pcpro.atelier.features.paiement.domain.Paiement;
import pcpro.atelier.features.paiement.infra.PaiementRepository;
import pcpro.atelier.features.vente.domain.Vente;

/**
 * Facture et note de crédit en PDF. Les montants facturés sont ceux figés à l'émission ; les paiements
 * (et donc le reste à payer) sont ceux du jour de l'impression.
 * Une facture de réparation sert aussi de bon de garantie.
 */
@Service
@Transactional(readOnly = true)
public class FacturePdfService {

  private final FactureService factureService;
  private final FactureRepository factureRepository;
  private final PaiementRepository paiementRepository;
  private final AtelierProperties atelier;
  private final PdfRenderer renderer;

  public FacturePdfService(
      FactureService factureService,
      FactureRepository factureRepository,
      PaiementRepository paiementRepository,
      AtelierProperties atelier,
      PdfRenderer renderer
  ) {
    this.factureService = factureService;
    this.factureRepository = factureRepository;
    this.paiementRepository = paiementRepository;
    this.atelier = atelier;
    this.renderer = renderer;
  }

  public record FacturePdf(String nomFichier, byte[] pdf) {
  }

  public enum Statut {
    PAYEE,
    A_PAYER,
    ANNULEE,
    NOTE_CREDIT
  }

  public record PaiementVue(LocalDate date, String moyen, BigDecimal montant) {
  }

  /** Données imprimées. */
  public record Vue(
      boolean noteCredit,
      String numero,
      LocalDate dateEmission,
      Statut statut,
      String clientNom,
      String clientAdresse,
      String clientTva,
      String clientEmail,
      String clientTelephone,
      String objet,
      String reference,
      String appareil,
      LocalDate datePrestation,
      List<Montants.Ligne> lignes,
      Montants.Totaux totaux,
      List<PaiementVue> paiements,
      BigDecimal dejaPaye,
      BigDecimal reste,
      String annuleePar,
      String factureOrigine,
      LocalDate factureOrigineDate,
      String communication,
      String qrVirement,
      LocalDate finGarantie
  ) {
  }

  public FacturePdf generer(Long factureId) {
    return generer(factureService.getOrThrow(factureId));
  }

  public FacturePdf generer(Facture facture) {
    String type = facture.estNoteCredit() ? "Note de crédit" : "Facture";
    byte[] pdf = renderer.rendre("facture", type + " " + facture.getNumero(), Map.of(
        "f", vue(facture),
        "mentionFranchise", Montants.MENTION_FRANCHISE));
    return new FacturePdf((facture.estNoteCredit() ? "note-credit-" : "facture-") + facture.getNumero() + ".pdf", pdf);
  }

  Vue vue(Facture facture) {
    Dossier dossier = facture.getDossier();
    Vente vente = facture.getVente();

    List<PaiementVue> paiements = List.of();
    if (!facture.estNoteCredit()) {
      if (dossier != null) {
        List<Paiement> liste = paiementRepository.findByDossier(dossier);
        paiements = liste.stream()
            .map(p -> new PaiementVue(Formats.jour(p.getCreatedAt()), moyen(p.getMoyen()), p.getMontant()))
            .toList();
      } else if (vente != null) {
        paiements = List.of(new PaiementVue(Formats.jour(vente.getCreatedAt()), moyen(vente.getMoyen()), vente.total()));
      }
    }
    BigDecimal dejaPaye = paiements.stream().map(PaiementVue::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal reste = facture.getTotal().subtract(dejaPaye).max(BigDecimal.ZERO);

    String annuleePar = facture.estNoteCredit() ? null
        : factureRepository.findByFactureOrigineId(facture.getId()).map(Facture::getNumero).orElse(null);
    Statut statut = facture.estNoteCredit() ? Statut.NOTE_CREDIT
        : annuleePar != null ? Statut.ANNULEE
        : reste.signum() > 0 ? Statut.A_PAYER
        : Statut.PAYEE;

    String communication = facture.estNoteCredit() ? null
        : QrCodes.communicationStructuree(facture.getAnnee() * 1_000_000L + facture.getSequence());
    String beneficiaire = atelier.raisonSociale().isBlank() ? atelier.nom() : atelier.raisonSociale();
    String qrVirement = statut == Statut.A_PAYER
        ? QrCodes.virement(beneficiaire, atelier.iban(), atelier.bic(), reste, communication)
        : null;

    LocalDate finGarantie = dossier != null && !facture.estNoteCredit() ? dossier.getFinGarantie() : null;

    return new Vue(
        facture.estNoteCredit(),
        facture.getNumero(),
        facture.getDateEmission(),
        statut,
        facture.getClientNom(),
        facture.getClientAdresse(),
        facture.getClientTva(),
        facture.getClientEmail(),
        dossier != null ? dossier.getClient().getTelephone() : null,
        dossier != null ? "Réparation" : "Vente au comptoir",
        dossier != null ? dossier.getReference() : null,
        dossier != null
            ? Formats.INSTANCE.majuscule(Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele()))
            : null,
        datePrestation(dossier, vente),
        facture.getLignes().stream()
            .map(l -> new Montants.Ligne(l.getLibelle(), l.getDetail(), l.getQuantite(), l.getPrixUnitaire(), l.getTotal()))
            .toList(),
        Montants.Totaux.of(facture.getTotal(), facture.getRegimeTva(), facture.getTauxTva()),
        paiements,
        dejaPaye,
        reste,
        annuleePar,
        facture.getFactureOrigine() != null ? facture.getFactureOrigine().getNumero() : null,
        facture.getFactureOrigine() != null ? facture.getFactureOrigine().getDateEmission() : null,
        statut == Statut.A_PAYER ? communication : null,
        qrVirement,
        finGarantie
    );
  }

  /** Date de la réparation (appareil prêt) ou de la vente. */
  private static LocalDate datePrestation(Dossier dossier, Vente vente) {
    if (dossier != null) {
      return dossier.getPretAt() != null ? Formats.jour(dossier.getPretAt())
          : dossier.getClosedAt() != null ? Formats.jour(dossier.getClosedAt()) : null;
    }
    return vente != null ? Formats.jour(vente.getCreatedAt()) : null;
  }

  static String moyen(MoyenPaiement moyen) {
    return switch (moyen) {
      case ESPECES -> "Espèces";
      case BANCONTACT -> "Bancontact";
      case VIREMENT -> "Virement";
      case AUTRE -> "Autre";
    };
  }
}

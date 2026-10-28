package pcpro.atelier.features.bondepot;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.document.Formats;
import pcpro.atelier.features.document.PdfRenderer;
import pcpro.atelier.features.document.QrCodes;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;

/**
 * Bon de dépôt remis au client avec son appareil : récapitulatif du dépôt, QR code vers le suivi en ligne,
 * conditions et signature. L'exemplaire atelier se termine par une étiquette à coller sur l'appareil.
 */
@Service
@Transactional(readOnly = true)
public class BonDepotService {

  private final DossierRepository dossierRepository;
  private final AtelierProperties atelier;
  private final PdfRenderer renderer;

  public BonDepotService(DossierRepository dossierRepository, AtelierProperties atelier, PdfRenderer renderer) {
    this.dossierRepository = dossierRepository;
    this.atelier = atelier;
    this.renderer = renderer;
  }

  public record BonDepot(String nomFichier, byte[] pdf) {
  }

  /** Une page par exemplaire : le client garde le sien, l'atelier garde celui signé par le client. */
  public enum Exemplaire {
    CLIENT,
    ATELIER
  }

  /** Données imprimées, déjà mises en forme. */
  public record Vue(
      String reference,
      LocalDate dateDepot,
      LocalDate datePrevue,
      String clientNom,
      String clientTelephone,
      String clientEmail,
      String appareil,
      String numeroSerie,
      String accessoires,
      String probleme,
      String codeSuivi,
      String urlSuivi,
      String qrSuivi,
      String qrDossier,
      List<String> conditions
  ) {
  }

  /** Les deux exemplaires, à imprimer. */
  public BonDepot generer(Long dossierId) {
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    return generer(dossier, List.of(Exemplaire.CLIENT, Exemplaire.ATELIER));
  }

  public BonDepot generer(Dossier dossier, List<Exemplaire> exemplaires) {
    byte[] pdf = renderer.rendre("bon-depot", "Bon de dépôt " + dossier.getReference(),
        Map.of("d", vue(dossier), "exemplaires", exemplaires));
    return new BonDepot("bon-depot-" + dossier.getReference() + ".pdf", pdf);
  }

  private Vue vue(Dossier dossier) {
    Client client = dossier.getClient();
    String lienSuivi = atelier.lienSuivi(dossier.getReference(), dossier.getCodeSuivi());
    String lienDossier = atelier.lienDossier(dossier.getId());
    return new Vue(
        dossier.getReference(),
        dossier.getDateEntree(),
        dossier.getDateEstimee(),
        client.getNom(),
        client.getTelephone(),
        client.getEmail(),
        Formats.INSTANCE.majuscule(Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele())),
        dossier.getNumeroSerie(),
        dossier.getAccessoires(),
        dossier.getDescriptionProbleme(),
        dossier.getCodeSuivi(),
        atelier.urlPublique().isBlank() ? null : atelier.urlPublique().replaceFirst("^https?://", "") + "/suivi",
        lienSuivi != null ? QrCodes.dataUri(lienSuivi) : null,
        lienDossier != null ? QrCodes.dataUri(lienDossier) : null,
        conditions()
    );
  }

  /** Conditions configurées (une par ligne), ou conditions par défaut d'un atelier de réparation. */
  private List<String> conditions() {
    if (!atelier.conditionsDepot().isBlank()) {
      return Arrays.stream(atelier.conditionsDepot().split("\\R|\\\\n"))
          .map(String::trim)
          .filter(l -> !l.isEmpty())
          .toList();
    }
    String garantie = atelier.garantieMois() > 0
        ? "Les réparations sont garanties " + atelier.garantieMois()
            + " mois (pièces et main-d'œuvre), hors casse, oxydation, mauvaise utilisation ou intervention d'un tiers."
        : null;
    return java.util.stream.Stream.of(
            "Le devis est gratuit. Aucune réparation payante n'est réalisée sans votre accord.",
            "Sauvegardez vos données avant le dépôt : l'atelier ne peut être tenu responsable d'une perte de données.",
            garantie,
            "L'appareil est rendu sur présentation de ce bon ou d'une pièce d'identité au nom du client.",
            "Merci de reprendre votre appareil dans les 30 jours qui suivent notre avis de fin de réparation.")
        .filter(c -> c != null)
        .toList();
  }
}

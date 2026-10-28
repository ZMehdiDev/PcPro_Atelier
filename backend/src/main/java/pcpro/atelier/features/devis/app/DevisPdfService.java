package pcpro.atelier.features.devis.app;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.infra.DevisRepository;
import pcpro.atelier.features.document.Formats;
import pcpro.atelier.features.document.Montants;
import pcpro.atelier.features.document.PdfRenderer;
import pcpro.atelier.features.document.QrCodes;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;

/**
 * Devis en PDF : imprimé pour le client au comptoir, ou joint à l'email du devis.
 */
@Service
@Transactional(readOnly = true)
public class DevisPdfService {

  private final DossierRepository dossierRepository;
  private final DevisRepository devisRepository;
  private final AtelierProperties atelier;
  private final PdfRenderer renderer;
  private final Clock clock;

  public DevisPdfService(
      DossierRepository dossierRepository,
      DevisRepository devisRepository,
      AtelierProperties atelier,
      PdfRenderer renderer,
      Clock clock
  ) {
    this.dossierRepository = dossierRepository;
    this.devisRepository = devisRepository;
    this.atelier = atelier;
    this.renderer = renderer;
    this.clock = clock;
  }

  public record DevisPdf(String nomFichier, byte[] pdf) {
  }

  /** Données imprimées. */
  public record Vue(
      String reference,
      LocalDate date,
      StatutDevis statut,
      LocalDate valableJusquau,
      int validiteJours,
      LocalDate decideLe,
      String clientNom,
      String clientTelephone,
      String clientEmail,
      String appareil,
      String numeroSerie,
      List<Montants.Ligne> lignes,
      Montants.Totaux totaux,
      String commentaire,
      String qrAccord
  ) {
  }

  public DevisPdf generer(Long dossierId) {
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    Devis devis = devisRepository.findByDossier(dossier)
        .orElseThrow(() -> new ResourceNotFoundException("Aucun devis pour le dossier " + dossier.getReference()));
    return generer(dossier, devis);
  }

  public DevisPdf generer(Dossier dossier, Devis devis) {
    byte[] pdf = renderer.rendre("devis", "Devis " + dossier.getReference(), Map.of(
        "d", vue(dossier, devis),
        "mentionFranchise", Montants.MENTION_FRANCHISE));
    return new DevisPdf("devis-" + dossier.getReference() + ".pdf", pdf);
  }

  private Vue vue(Dossier dossier, Devis devis) {
    LocalDate date = jour(devis.getUpdatedAt());
    String lienSuivi = devis.getStatut() == StatutDevis.EN_ATTENTE
        ? atelier.lienSuivi(dossier.getReference(), dossier.getCodeSuivi())
        : null;
    return new Vue(
        dossier.getReference(),
        date,
        devis.getStatut(),
        date.plusDays(atelier.devisValiditeJours()),
        atelier.devisValiditeJours(),
        devis.getDecideAt() != null ? jour(devis.getDecideAt()) : null,
        dossier.getClient().getNom(),
        dossier.getClient().getTelephone(),
        dossier.getClient().getEmail(),
        Formats.INSTANCE.majuscule(Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele())),
        dossier.getNumeroSerie(),
        devis.getLignes().stream()
            .map(l -> new Montants.Ligne(l.getLibelle(), Montants.type(l.getType()), l.getQuantite(),
                l.getPrixUnitaire(), l.total()))
            .toList(),
        Montants.Totaux.of(devis.total(), atelier.tva().regime(), atelier.tva().taux()),
        devis.getCommentaire(),
        lienSuivi != null ? QrCodes.dataUri(lienSuivi) : null
    );
  }

  /** Jour (dans le fuseau de l'atelier) d'un instant ; aujourd'hui pour un devis pas encore enregistré. */
  private LocalDate jour(OffsetDateTime instant) {
    return instant == null ? LocalDate.now(clock) : instant.atZoneSameInstant(clock.getZone()).toLocalDate();
  }
}

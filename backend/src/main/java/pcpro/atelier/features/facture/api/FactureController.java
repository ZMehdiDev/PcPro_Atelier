package pcpro.atelier.features.facture.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.document.PdfReponse;
import pcpro.atelier.features.facture.api.dto.EnvoiFactureRequest;
import pcpro.atelier.features.facture.api.dto.FactureDto;
import pcpro.atelier.features.facture.api.dto.FactureRequest;
import pcpro.atelier.features.facture.app.FacturePdfService;
import pcpro.atelier.features.facture.app.FactureService;
import pcpro.atelier.features.facture.app.JournalRecettesService;
import pcpro.atelier.features.notification.NotificationClientService;

@RestController
public class FactureController {

  private final FactureService factureService;
  private final FacturePdfService facturePdfService;
  private final JournalRecettesService journalRecettesService;
  private final NotificationClientService notificationService;

  public FactureController(
      FactureService factureService,
      FacturePdfService facturePdfService,
      JournalRecettesService journalRecettesService,
      NotificationClientService notificationService
  ) {
    this.factureService = factureService;
    this.facturePdfService = facturePdfService;
    this.journalRecettesService = journalRecettesService;
    this.notificationService = notificationService;
  }

  /** Facture la réparation (ou renvoie sa facture si elle existe déjà). Le corps est facultatif. */
  @PostMapping("/api/dossiers/{dossierId}/facture")
  public FactureDto facturerDossier(@PathVariable Long dossierId, @Valid @RequestBody(required = false) FactureRequest request) {
    return factureService.facturerDossier(dossierId, request != null ? request : FactureRequest.VIDE);
  }

  /** Facture valable du dossier, ou 204 s'il n'en a pas. */
  @GetMapping("/api/dossiers/{dossierId}/facture")
  public ResponseEntity<FactureDto> factureDuDossier(@PathVariable Long dossierId) {
    return reponse(factureService.duDossier(dossierId));
  }

  @PostMapping("/api/ventes/{venteId}/facture")
  public FactureDto facturerVente(@PathVariable Long venteId, @Valid @RequestBody FactureRequest request) {
    return factureService.facturerVente(venteId, request);
  }

  @GetMapping("/api/ventes/{venteId}/facture")
  public ResponseEntity<FactureDto> factureDeLaVente(@PathVariable Long venteId) {
    return reponse(factureService.deLaVente(venteId));
  }

  /** Factures et notes de crédit émises sur la période (ce mois-ci par défaut). */
  @GetMapping("/api/factures")
  @PreAuthorize("hasRole('ADMIN')")
  public List<FactureDto> lister(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
  ) {
    return factureService.lister(du, au);
  }

  /** Journal des recettes de la période, en PDF pour le comptable (ce mois-ci par défaut). */
  @GetMapping("/api/factures/journal")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> journal(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
  ) {
    JournalRecettesService.Journal journal = journalRecettesService.generer(du, au);
    return PdfReponse.inline(journal.nomFichier(), journal.pdf());
  }

  @GetMapping("/api/factures/{id}")
  public FactureDto get(@PathVariable Long id) {
    return factureService.get(id);
  }

  @GetMapping("/api/factures/{id}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    FacturePdfService.FacturePdf facture = facturePdfService.generer(id);
    return PdfReponse.inline(facture.nomFichier(), facture.pdf());
  }

  /** Envoie la facture en PDF au client (à l'email donné, sinon à celui noté sur la facture). */
  @PostMapping("/api/factures/{id}/envoi")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void envoyer(@PathVariable Long id, @Valid @RequestBody(required = false) EnvoiFactureRequest request) {
    notificationService.envoyerFacture(id, request != null ? request.email() : null);
  }

  /** Annule la facture par une note de crédit (erreur, remboursement). Une nouvelle facture peut ensuite être émise. */
  @PostMapping("/api/factures/{id}/annulation")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public FactureDto annuler(@PathVariable Long id) {
    return factureService.annuler(id);
  }

  private static ResponseEntity<FactureDto> reponse(FactureDto facture) {
    return facture != null ? ResponseEntity.ok(facture) : ResponseEntity.noContent().build();
  }
}

package pcpro.atelier.features.devis.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.devis.api.dto.DecisionDevisRequest;
import pcpro.atelier.features.devis.api.dto.DevisDto;
import pcpro.atelier.features.devis.api.dto.DevisRequest;
import pcpro.atelier.features.devis.app.DevisPdfService;
import pcpro.atelier.features.devis.app.DevisService;
import pcpro.atelier.features.document.PdfReponse;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/devis")
public class DevisController {

  private final DevisService devisService;
  private final DevisPdfService devisPdfService;

  public DevisController(DevisService devisService, DevisPdfService devisPdfService) {
    this.devisService = devisService;
    this.devisPdfService = devisPdfService;
  }

  /** Crée ou remplace le devis (tant qu'il n'est pas accepté). Le devis est aussi dans le détail du dossier. */
  @PutMapping
  public DevisDto enregistrer(@PathVariable Long dossierId, @Valid @RequestBody DevisRequest request) {
    return devisService.enregistrer(dossierId, request);
  }

  @PostMapping("/decision")
  public DevisDto decider(@PathVariable Long dossierId, @Valid @RequestBody DecisionDevisRequest request) {
    return devisService.decider(dossierId, request.accepte(), request.commentaire());
  }

  /** Devis en PDF, à imprimer ou à remettre au client. */
  @GetMapping("/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable Long dossierId) {
    DevisPdfService.DevisPdf devis = devisPdfService.generer(dossierId);
    return PdfReponse.inline(devis.nomFichier(), devis.pdf());
  }

  /** Envoie le devis au client par email, avec le PDF joint et le lien pour l'accepter ou le refuser en ligne. */
  @PostMapping("/envoi")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void envoyer(@PathVariable Long dossierId) {
    devisService.envoyerParEmail(dossierId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void supprimer(@PathVariable Long dossierId) {
    devisService.supprimer(dossierId);
  }
}

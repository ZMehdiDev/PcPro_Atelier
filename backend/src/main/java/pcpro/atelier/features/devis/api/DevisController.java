package pcpro.atelier.features.devis.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.devis.api.dto.DecisionDevisRequest;
import pcpro.atelier.features.devis.api.dto.DevisDto;
import pcpro.atelier.features.devis.api.dto.DevisRequest;
import pcpro.atelier.features.devis.app.DevisService;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/devis")
public class DevisController {

  private final DevisService devisService;

  public DevisController(DevisService devisService) {
    this.devisService = devisService;
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

  /** Envoie le devis au client par email, avec le lien pour l'accepter ou le refuser en ligne. */
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

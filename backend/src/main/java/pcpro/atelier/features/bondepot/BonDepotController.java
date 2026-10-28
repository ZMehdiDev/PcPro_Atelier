package pcpro.atelier.features.bondepot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import pcpro.atelier.features.document.PdfReponse;

@RestController
public class BonDepotController {

  private final BonDepotService bonDepotService;

  public BonDepotController(BonDepotService bonDepotService) {
    this.bonDepotService = bonDepotService;
  }

  /** PDF à imprimer (s'ouvre dans le navigateur) : exemplaire client et exemplaire atelier avec l'étiquette. */
  @GetMapping("/api/dossiers/{id}/bon-depot")
  public ResponseEntity<byte[]> bonDepot(@PathVariable Long id) {
    BonDepotService.BonDepot bon = bonDepotService.generer(id);
    return PdfReponse.inline(bon.nomFichier(), bon.pdf());
  }
}

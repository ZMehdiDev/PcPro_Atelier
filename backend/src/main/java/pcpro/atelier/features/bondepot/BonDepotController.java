package pcpro.atelier.features.bondepot;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BonDepotController {

  private final BonDepotService bonDepotService;

  public BonDepotController(BonDepotService bonDepotService) {
    this.bonDepotService = bonDepotService;
  }

  /** PDF à imprimer (s'ouvre dans le navigateur). */
  @GetMapping("/api/dossiers/{id}/bon-depot")
  public ResponseEntity<byte[]> bonDepot(@PathVariable Long id) {
    BonDepotService.BonDepot bon = bonDepotService.generer(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(bon.nomFichier()).build().toString())
        .body(bon.pdf());
  }
}

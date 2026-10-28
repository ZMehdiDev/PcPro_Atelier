package pcpro.atelier.features.document;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Réponse HTTP d'un PDF à ouvrir dans le navigateur (pour l'imprimer), jamais gardé en cache. */
public final class PdfReponse {

  private PdfReponse() {
  }

  public static ResponseEntity<byte[]> inline(String nomFichier, byte[] pdf) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nomFichier).build().toString())
        .body(pdf);
  }
}

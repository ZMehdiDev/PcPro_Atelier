package pcpro.atelier.features.photo.api;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import pcpro.atelier.features.photo.api.dto.PhotoDto;
import pcpro.atelier.features.photo.app.PhotoService;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/photos")
public class PhotoController {

  private final PhotoService photoService;

  public PhotoController(PhotoService photoService) {
    this.photoService = photoService;
  }

  /**
   * Upload multipart : champ "file" (JPEG, PNG ou WebP, 10 MB max) et "description" optionnelle.
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public PhotoDto ajouter(
      @PathVariable Long dossierId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "description", required = false) String description
  ) {
    return photoService.ajouter(dossierId, file, description);
  }

  @GetMapping("/{photoId}/contenu")
  public ResponseEntity<Resource> contenu(@PathVariable Long dossierId, @PathVariable Long photoId) {
    PhotoService.ContenuPhoto photo = photoService.contenu(dossierId, photoId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(photo.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(photo.nomFichier()).build().toString())
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
        .body(photo.resource());
  }

  @DeleteMapping("/{photoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void supprimer(@PathVariable Long dossierId, @PathVariable Long photoId) {
    photoService.supprimer(dossierId, photoId);
  }
}

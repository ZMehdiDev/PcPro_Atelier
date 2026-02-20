package pcpro.atelier.features.photo.api;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.service.FileStorageService;
import pcpro.atelier.features.photo.domain.Photo;
import pcpro.atelier.features.photo.infra.PhotoRepository;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

  private final PhotoRepository photoRepository;
  private final FileStorageService fileStorageService;

  public PhotoController(PhotoRepository photoRepository, FileStorageService fileStorageService) {
    this.photoRepository = photoRepository;
    this.fileStorageService = fileStorageService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
    // Trouver la photo
    Photo photo = photoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Photo", id));
    
    // Charger le fichier depuis le disque
    byte[] fileBytes = fileStorageService.loadFile(photo.getCheminFichier());
    
    // Créer la ressource
    ByteArrayResource resource = new ByteArrayResource(fileBytes);
    
    // Déterminer le type MIME
    MediaType mediaType = MediaType.parseMediaType(
        photo.getMimeType() != null ? photo.getMimeType() : "application/octet-stream"
    );
    
    // Retourner la réponse avec les headers appropriés
    return ResponseEntity.ok()
        .contentType(mediaType)
        .contentLength(fileBytes.length)
        .header(HttpHeaders.CONTENT_DISPOSITION, 
            "inline; filename=\"" + photo.getCheminFichier().substring(
                photo.getCheminFichier().lastIndexOf('/') + 1
            ) + "\"")
        .body(resource);
  }
}

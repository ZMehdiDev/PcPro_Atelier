package pcpro.atelier.features.photo.app;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.service.FileStorageService;
import pcpro.atelier.features.dossier.app.DossierMapper;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.photo.api.dto.PhotoDto;
import pcpro.atelier.features.photo.domain.Photo;
import pcpro.atelier.features.photo.infra.PhotoRepository;

@Service
@Transactional
public class PhotoService {

  public record ContenuPhoto(Resource resource, String mimeType, String nomFichier) {
  }

  private final DossierService dossierService;
  private final PhotoRepository photoRepository;
  private final FileStorageService fileStorageService;
  private final EvenementService evenementService;

  public PhotoService(
      DossierService dossierService,
      PhotoRepository photoRepository,
      FileStorageService fileStorageService,
      EvenementService evenementService
  ) {
    this.dossierService = dossierService;
    this.photoRepository = photoRepository;
    this.fileStorageService = fileStorageService;
    this.evenementService = evenementService;
  }

  public PhotoDto ajouter(Long dossierId, MultipartFile file, String description) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Le fichier est vide");
    }
    if (description != null && description.length() > 200) {
      throw new BadRequestException("La description ne peut pas dépasser 200 caractères");
    }

    byte[] contenu;
    try {
      contenu = file.getBytes();
    } catch (IOException e) {
      throw new BadRequestException("Impossible de lire le fichier envoyé");
    }

    FileStorageService.FichierStocke fichier = fileStorageService.stocker(contenu, dossier.getId());
    // Si la transaction échoue ensuite, on ne laisse pas de fichier orphelin sur le disque
    apresRollback(() -> fileStorageService.supprimer(fichier.cheminRelatif()));

    Photo photo = new Photo();
    photo.setDossier(dossier);
    photo.setCheminFichier(fichier.cheminRelatif());
    photo.setMimeType(fichier.type().mimeType());
    photo.setTailleBytes(fichier.tailleBytes());
    photo.setDescription(description == null || description.isBlank() ? null : description.trim());
    photo = photoRepository.save(photo);

    evenementService.journaliser(dossier, TypeActionEvenement.AJOUT_PHOTO, null,
        "Photo ajoutée (ID: " + photo.getId() + ", " + (fichier.tailleBytes() / 1024) + " KB)",
        photo.getDescription());
    return DossierMapper.toPhotoDto(photo);
  }

  @Transactional(readOnly = true)
  public ContenuPhoto contenu(Long dossierId, Long photoId) {
    Photo photo = getPhotoDuDossier(dossierId, photoId);
    String chemin = photo.getCheminFichier();
    return new ContenuPhoto(
        fileStorageService.charger(chemin),
        photo.getMimeType(),
        chemin.substring(chemin.lastIndexOf('/') + 1)
    );
  }

  public void supprimer(Long dossierId, Long photoId) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Photo photo = getPhotoDuDossier(dossierId, photoId);
    String chemin = photo.getCheminFichier();

    photoRepository.delete(photo);
    evenementService.journaliser(dossier, TypeActionEvenement.SUPPRESSION_PHOTO,
        "Photo ID: " + photoId, null, photo.getDescription());
    // Le fichier n'est supprimé qu'une fois la suppression en base validée
    apresCommit(() -> fileStorageService.supprimer(chemin));
  }

  private Photo getPhotoDuDossier(Long dossierId, Long photoId) {
    return photoRepository.findById(photoId)
        .filter(p -> p.getDossier().getId().equals(dossierId))
        .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
  }

  private static void apresCommit(Runnable action) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }

  private static void apresRollback(Runnable action) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) {
          action.run();
        }
      }
    });
  }
}

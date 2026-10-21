package pcpro.atelier.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import pcpro.atelier.core.config.FileStorageProperties;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ResourceNotFoundException;

/**
 * Stockage des photos sur disque, un sous-répertoire par dossier.
 *
 * Le type de fichier est déterminé à partir de son contenu (signature binaire), pas du Content-Type
 * envoyé par le client : un fichier qui n'est pas réellement une image JPEG/PNG/WebP est refusé.
 */
@Service
public class FileStorageService {

  private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

  /** Formats d'image acceptés. */
  public enum TypeImage {
    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png"),
    WEBP("image/webp", ".webp");

    private final String mimeType;
    private final String extension;

    TypeImage(String mimeType, String extension) {
      this.mimeType = mimeType;
      this.extension = extension;
    }

    public String mimeType() {
      return mimeType;
    }
  }

  public record FichierStocke(String cheminRelatif, TypeImage type, long tailleBytes) {
  }

  private final FileStorageProperties properties;
  private final Path photosDirectory;

  public FileStorageService(FileStorageProperties properties) {
    this.properties = properties;
    this.photosDirectory = Paths.get(properties.getPhotosDirectory()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(photosDirectory);
      logger.info("Répertoire de stockage des photos : {}", photosDirectory);
    } catch (IOException e) {
      throw new IllegalStateException("Impossible de créer le répertoire de stockage " + photosDirectory, e);
    }
  }

  /**
   * Vérifie puis écrit l'image. Le nom du fichier est généré (UUID), le nom d'origine n'est pas utilisé.
   */
  public FichierStocke stocker(byte[] contenu, Long dossierId) {
    if (contenu == null || contenu.length == 0) {
      throw new BadRequestException("Le fichier est vide");
    }
    if (contenu.length > properties.getMaxFileSizeBytes()) {
      throw new BadRequestException(String.format("Fichier trop volumineux (maximum %.0f MB)",
          properties.getMaxFileSizeBytes() / (1024.0 * 1024.0)));
    }
    TypeImage type = detecterType(contenu);

    try {
      Path dossierDir = photosDirectory.resolve("dossier-" + dossierId);
      Files.createDirectories(dossierDir);
      Path fichier = dossierDir.resolve(UUID.randomUUID() + type.extension);
      Files.write(fichier, contenu, StandardOpenOption.CREATE_NEW);

      String cheminRelatif = photosDirectory.relativize(fichier).toString().replace('\\', '/');
      logger.info("Photo stockée : {} ({} octets, {})", cheminRelatif, contenu.length, type.mimeType);
      return new FichierStocke(cheminRelatif, type, contenu.length);
    } catch (IOException e) {
      throw new IllegalStateException("Erreur lors de l'écriture de la photo", e);
    }
  }

  public Resource charger(String cheminRelatif) {
    Path fichier = resoudre(cheminRelatif);
    if (!Files.isRegularFile(fichier)) {
      logger.warn("Photo absente du disque : {}", cheminRelatif);
      throw new ResourceNotFoundException("Le fichier de cette photo est introuvable");
    }
    return new FileSystemResource(fichier);
  }

  /**
   * Supprime le fichier. N'échoue pas si le fichier n'existe plus (l'erreur est seulement loggée).
   */
  public void supprimer(String cheminRelatif) {
    try {
      if (Files.deleteIfExists(resoudre(cheminRelatif))) {
        logger.info("Photo supprimée du disque : {}", cheminRelatif);
      }
    } catch (IOException | RuntimeException e) {
      logger.error("Impossible de supprimer la photo {}", cheminRelatif, e);
    }
  }

  /** Empêche de sortir du répertoire de stockage (ex : "../../etc/passwd"). */
  private Path resoudre(String cheminRelatif) {
    Path fichier = photosDirectory.resolve(cheminRelatif).normalize();
    if (!fichier.startsWith(photosDirectory)) {
      throw new IllegalStateException("Chemin de photo hors du répertoire de stockage : " + cheminRelatif);
    }
    return fichier;
  }

  static TypeImage detecterType(byte[] c) {
    if (commencePar(c, 0, 0xFF, 0xD8, 0xFF)) {
      return TypeImage.JPEG;
    }
    if (commencePar(c, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
      return TypeImage.PNG;
    }
    if (commencePar(c, 0, 'R', 'I', 'F', 'F') && commencePar(c, 8, 'W', 'E', 'B', 'P')) {
      return TypeImage.WEBP;
    }
    throw new BadRequestException("Format non supporté : seules les images JPEG, PNG et WebP sont acceptées");
  }

  private static boolean commencePar(byte[] contenu, int offset, int... signature) {
    if (contenu.length < offset + signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if ((contenu[offset + i] & 0xFF) != signature[i]) {
        return false;
      }
    }
    return true;
  }
}

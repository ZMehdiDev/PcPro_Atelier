package pcpro.atelier.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pcpro.atelier.core.config.FileStorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

  private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
  
  private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
      "image/jpeg",
      "image/jpg",
      "image/png",
      "image/webp"
  );

  private final FileStorageProperties properties;
  private final Path photosDirectory;

  public FileStorageService(FileStorageProperties properties) {
    this.properties = properties;
    this.photosDirectory = Paths.get(properties.getPhotosDirectory()).toAbsolutePath().normalize();
    initializeDirectory();
  }

  private void initializeDirectory() {
    try {
      Files.createDirectories(photosDirectory);
      logger.info("Répertoire de stockage des photos initialisé : {}", photosDirectory);
    } catch (IOException e) {
      logger.error("Impossible de créer le répertoire de stockage : {}", photosDirectory, e);
      throw new RuntimeException("Erreur d'initialisation du stockage", e);
    }
  }

  /**
   * Valide le type MIME du fichier.
   */
  public void validateMimeType(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      throw new IllegalArgumentException("Le type MIME est requis");
    }
    
    if (!ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
      throw new IllegalArgumentException(
          String.format(
              "Type MIME non autorisé : %s. Types autorisés : %s",
              mimeType,
              ALLOWED_MIME_TYPES
          )
      );
    }
  }

  /**
   * Valide la taille du fichier.
   */
  public void validateFileSize(long fileSize) {
    if (fileSize <= 0) {
      throw new IllegalArgumentException("Le fichier est vide");
    }
    
    if (fileSize > properties.getMaxFileSizeBytes()) {
      throw new IllegalArgumentException(
          String.format(
              "Fichier trop volumineux : %d bytes. Taille maximale autorisée : %d bytes (%.2f MB)",
              fileSize,
              properties.getMaxFileSizeBytes(),
              properties.getMaxFileSizeBytes() / (1024.0 * 1024.0)
          )
      );
    }
  }

  /**
   * Stocke un fichier et retourne le chemin relatif.
   * 
   * @param fileBytes Les bytes du fichier
   * @param originalFilename Le nom original du fichier
   * @param mimeType Le type MIME
   * @param dossierId L'ID du dossier (pour organisation)
   * @return Le chemin relatif du fichier stocké
   */
  public String storeFile(byte[] fileBytes, String originalFilename, String mimeType, Long dossierId) {
    // Valider le type MIME
    validateMimeType(mimeType);
    
    // Valider la taille
    validateFileSize(fileBytes.length);
    
    try {
      // Créer un sous-répertoire par dossier pour organisation
      Path dossierDir = photosDirectory.resolve("dossier-" + dossierId);
      Files.createDirectories(dossierDir);
      
      // Générer un nom de fichier unique
      String extension = getExtensionFromMimeType(mimeType);
      String uniqueFilename = UUID.randomUUID().toString() + extension;
      
      // Chemin complet du fichier
      Path filePath = dossierDir.resolve(uniqueFilename);
      
      // Écrire le fichier
      Files.write(filePath, fileBytes);
      
      // Retourner le chemin relatif (pour stockage en DB)
      Path relativePath = photosDirectory.relativize(filePath);
      logger.info("Fichier stocké : {} ({} bytes, {})", relativePath, fileBytes.length, mimeType);
      
      return relativePath.toString().replace('\\', '/'); // Normaliser les séparateurs
      
    } catch (IOException e) {
      logger.error("Erreur lors du stockage du fichier : {}", originalFilename, e);
      throw new RuntimeException("Erreur lors du stockage du fichier", e);
    }
  }

  /**
   * Récupère le contenu d'un fichier.
   */
  public byte[] loadFile(String relativePath) {
    try {
      Path filePath = photosDirectory.resolve(relativePath).normalize();
      
      // Sécurité : vérifier que le chemin ne sort pas du répertoire de stockage
      if (!filePath.startsWith(photosDirectory)) {
        throw new IllegalArgumentException("Chemin de fichier invalide");
      }
      
      if (!Files.exists(filePath)) {
        throw new IllegalArgumentException("Fichier non trouvé : " + relativePath);
      }
      
      return Files.readAllBytes(filePath);
      
    } catch (IOException e) {
      logger.error("Erreur lors de la lecture du fichier : {}", relativePath, e);
      throw new RuntimeException("Erreur lors de la lecture du fichier", e);
    }
  }

  /**
   * Supprime un fichier.
   */
  public void deleteFile(String relativePath) {
    try {
      Path filePath = photosDirectory.resolve(relativePath).normalize();
      
      // Sécurité : vérifier que le chemin ne sort pas du répertoire de stockage
      if (!filePath.startsWith(photosDirectory)) {
        throw new IllegalArgumentException("Chemin de fichier invalide");
      }
      
      if (Files.exists(filePath)) {
        Files.delete(filePath);
        logger.info("Fichier supprimé : {}", relativePath);
      }
      
    } catch (IOException e) {
      logger.error("Erreur lors de la suppression du fichier : {}", relativePath, e);
      // Ne pas lever d'exception, juste logger
    }
  }

  private String getExtensionFromMimeType(String mimeType) {
    return switch (mimeType.toLowerCase()) {
      case "image/jpeg", "image/jpg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".bin";
    };
  }
}

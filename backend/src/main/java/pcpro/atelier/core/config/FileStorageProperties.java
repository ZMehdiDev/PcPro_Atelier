package pcpro.atelier.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {

  private String photosDirectory = "./uploads/photos";
  private long maxFileSizeBytes = 10 * 1024 * 1024; // 10MB par défaut

  public String getPhotosDirectory() {
    return photosDirectory;
  }

  public void setPhotosDirectory(String photosDirectory) {
    this.photosDirectory = photosDirectory;
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }
}

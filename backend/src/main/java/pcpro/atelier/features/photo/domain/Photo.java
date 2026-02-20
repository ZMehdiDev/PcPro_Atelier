package pcpro.atelier.features.photo.domain;

import jakarta.persistence.*;
import pcpro.atelier.features.dossier.domain.Dossier;
import java.time.OffsetDateTime;

@Entity
@Table(name = "photo")
public class Photo {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false)
  private Dossier dossier;

  @Column(name = "chemin_fichier", nullable = false, length = 500)
  private String cheminFichier;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @Column(name = "taille_bytes")
  private Long tailleBytes;

  @Column(length = 200)
  private String description;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public Long getId() { 
    return id; 
  }

  public Dossier getDossier() { 
    return dossier; 
  }

  public void setDossier(Dossier dossier) { 
    this.dossier = dossier; 
  }

  public String getCheminFichier() { 
    return cheminFichier; 
  }

  public void setCheminFichier(String cheminFichier) { 
    this.cheminFichier = cheminFichier; 
  }

  public String getDescription() { 
    return description; 
  }

  public void setDescription(String description) { 
    this.description = description; 
  }

  public OffsetDateTime getCreatedAt() { 
    return createdAt; 
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public Long getTailleBytes() {
    return tailleBytes;
  }

  public void setTailleBytes(Long tailleBytes) {
    this.tailleBytes = tailleBytes;
  }
}

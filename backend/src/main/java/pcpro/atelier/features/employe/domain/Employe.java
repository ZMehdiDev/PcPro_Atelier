package pcpro.atelier.features.employe.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employe")
public class Employe {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String prenom;

  @Column(nullable = false)
  private boolean actif = true;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public Long getId() { 
    return id; 
  }

  public String getPrenom() { 
    return prenom; 
  }

  public void setPrenom(String prenom) { 
    this.prenom = prenom; 
  }

  public boolean isActif() { 
    return actif; 
  }

  public void setActif(boolean actif) { 
    this.actif = actif; 
  }

  public OffsetDateTime getCreatedAt() { 
    return createdAt; 
  }
}

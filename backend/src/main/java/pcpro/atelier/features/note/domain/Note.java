package pcpro.atelier.features.note.domain;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import java.time.OffsetDateTime;

@Entity
@Table(name = "note")
public class Note {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false)
  private Dossier dossier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employe_id")
  private Employe auteur;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String contenu;

  @Column(nullable = false)
  private boolean sensible = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
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

  public Employe getAuteur() {
    return auteur;
  }

  public void setAuteur(Employe auteur) {
    this.auteur = auteur;
  }

  public String getContenu() {
    return contenu;
  }

  public void setContenu(String contenu) {
    this.contenu = contenu;
  }

  public boolean isSensible() {
    return sensible;
  }

  public void setSensible(boolean sensible) {
    this.sensible = sensible;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

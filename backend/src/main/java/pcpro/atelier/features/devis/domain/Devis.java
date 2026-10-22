package pcpro.atelier.features.devis.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.features.dossier.domain.Dossier;

/**
 * Devis d'un dossier (prix TTC). Il peut être refait tant que le client ne l'a pas accepté.
 */
@Entity
@Table(name = "devis")
public class Devis {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false, unique = true)
  private Dossier dossier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatutDevis statut;

  @Column(length = 500)
  private String commentaire;

  @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<DevisLigne> lignes = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "decide_at")
  private OffsetDateTime decideAt;

  public BigDecimal total() {
    return lignes.stream().map(DevisLigne::total).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void remplacerLignes(List<DevisLigne> nouvelles) {
    lignes.clear();
    for (int i = 0; i < nouvelles.size(); i++) {
      DevisLigne ligne = nouvelles.get(i);
      ligne.setDevis(this);
      ligne.setPosition(i);
      lignes.add(ligne);
    }
  }

  public Long getId() {
    return id;
  }

  public Dossier getDossier() {
    return dossier;
  }

  public void setDossier(Dossier dossier) {
    this.dossier = dossier;
  }

  public StatutDevis getStatut() {
    return statut;
  }

  public void setStatut(StatutDevis statut) {
    this.statut = statut;
  }

  public String getCommentaire() {
    return commentaire;
  }

  public void setCommentaire(String commentaire) {
    this.commentaire = commentaire;
  }

  public List<DevisLigne> getLignes() {
    return lignes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getDecideAt() {
    return decideAt;
  }

  public void setDecideAt(OffsetDateTime decideAt) {
    this.decideAt = decideAt;
  }
}

package pcpro.atelier.features.piece.domain;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.features.dossier.domain.Dossier;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "piece")
public class Piece {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false)
  private Dossier dossier;

  @Column(nullable = false, length = 120)
  private String reference;

  @Column(length = 120)
  private String fournisseur;

  private BigDecimal cout;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EtatPiece etat;

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

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getFournisseur() {
    return fournisseur;
  }

  public void setFournisseur(String fournisseur) {
    this.fournisseur = fournisseur;
  }

  public BigDecimal getCout() {
    return cout;
  }

  public void setCout(BigDecimal cout) {
    this.cout = cout;
  }

  public EtatPiece getEtat() {
    return etat;
  }

  public void setEtat(EtatPiece etat) {
    this.etat = etat;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

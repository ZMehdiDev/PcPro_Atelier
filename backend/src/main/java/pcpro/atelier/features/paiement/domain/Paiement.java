package pcpro.atelier.features.paiement.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;

/** Somme encaissée pour un dossier (acompte ou solde). */
@Entity
@Table(name = "paiement")
public class Paiement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false)
  private Dossier dossier;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal montant;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MoyenPaiement moyen;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "employe_id", nullable = false)
  private Employe employe;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public Long getId() {
    return id;
  }

  public Dossier getDossier() {
    return dossier;
  }

  public void setDossier(Dossier dossier) {
    this.dossier = dossier;
  }

  public BigDecimal getMontant() {
    return montant;
  }

  public void setMontant(BigDecimal montant) {
    this.montant = montant;
  }

  public MoyenPaiement getMoyen() {
    return moyen;
  }

  public void setMoyen(MoyenPaiement moyen) {
    this.moyen = moyen;
  }

  public Employe getEmploye() {
    return employe;
  }

  public void setEmploye(Employe employe) {
    this.employe = employe;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

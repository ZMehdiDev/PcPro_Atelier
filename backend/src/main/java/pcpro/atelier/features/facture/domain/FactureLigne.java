package pcpro.atelier.features.facture.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "facture_ligne")
public class FactureLigne {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "facture_id", nullable = false)
  private Facture facture;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false, length = 200)
  private String libelle;

  /** Précision sous le libellé (ex : "Main-d'œuvre", "Pièce"). */
  @Column(length = 100)
  private String detail;

  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal quantite;

  /** Prix unitaire TVAC. */
  @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
  private BigDecimal prixUnitaire;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  public FactureLigne() {
  }

  public FactureLigne(String libelle, String detail, BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal total) {
    this.libelle = libelle;
    this.detail = detail;
    this.quantite = quantite;
    this.prixUnitaire = prixUnitaire;
    this.total = total;
  }

  public Long getId() {
    return id;
  }

  void setFacture(Facture facture) {
    this.facture = facture;
  }

  public int getPosition() {
    return position;
  }

  void setPosition(int position) {
    this.position = position;
  }

  public String getLibelle() {
    return libelle;
  }

  public String getDetail() {
    return detail;
  }

  public BigDecimal getQuantite() {
    return quantite;
  }

  public BigDecimal getPrixUnitaire() {
    return prixUnitaire;
  }

  public BigDecimal getTotal() {
    return total;
  }
}

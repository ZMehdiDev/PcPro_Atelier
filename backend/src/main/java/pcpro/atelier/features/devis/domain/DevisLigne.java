package pcpro.atelier.features.devis.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.TypeLigneDevis;

@Entity
@Table(name = "devis_ligne")
public class DevisLigne {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "devis_id", nullable = false)
  private Devis devis;

  @Column(nullable = false)
  private int position;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TypeLigneDevis type;

  @Column(nullable = false, length = 200)
  private String libelle;

  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal quantite;

  @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
  private BigDecimal prixUnitaire;

  public DevisLigne() {
  }

  public DevisLigne(TypeLigneDevis type, String libelle, BigDecimal quantite, BigDecimal prixUnitaire) {
    this.type = type;
    this.libelle = libelle;
    this.quantite = quantite;
    this.prixUnitaire = prixUnitaire;
  }

  public BigDecimal total() {
    return quantite.multiply(prixUnitaire).setScale(2, RoundingMode.HALF_UP);
  }

  public Long getId() {
    return id;
  }

  public Devis getDevis() {
    return devis;
  }

  void setDevis(Devis devis) {
    this.devis = devis;
  }

  public int getPosition() {
    return position;
  }

  void setPosition(int position) {
    this.position = position;
  }

  public TypeLigneDevis getType() {
    return type;
  }

  public String getLibelle() {
    return libelle;
  }

  public BigDecimal getQuantite() {
    return quantite;
  }

  public BigDecimal getPrixUnitaire() {
    return prixUnitaire;
  }
}

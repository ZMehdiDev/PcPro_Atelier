package pcpro.atelier.features.vente.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "vente_ligne")
public class VenteLigne {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "vente_id", nullable = false)
  private Vente vente;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false, length = 200)
  private String article;

  @Column(nullable = false)
  private int quantite;

  @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
  private BigDecimal prixUnitaire;

  /** Prix d'achat d'un article, facultatif (inconnu : compté comme 0 dans la marge). */
  @Column(name = "cout_unitaire", precision = 10, scale = 2)
  private BigDecimal coutUnitaire;

  public VenteLigne() {
  }

  public VenteLigne(String article, int quantite, BigDecimal prixUnitaire, BigDecimal coutUnitaire) {
    this.article = article;
    this.quantite = quantite;
    this.prixUnitaire = prixUnitaire;
    this.coutUnitaire = coutUnitaire;
  }

  public BigDecimal total() {
    return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
  }

  public BigDecimal cout() {
    return coutUnitaire == null ? BigDecimal.ZERO : coutUnitaire.multiply(BigDecimal.valueOf(quantite));
  }

  public Long getId() {
    return id;
  }

  void setVente(Vente vente) {
    this.vente = vente;
  }

  void setPosition(int position) {
    this.position = position;
  }

  public String getArticle() {
    return article;
  }

  public int getQuantite() {
    return quantite;
  }

  public BigDecimal getPrixUnitaire() {
    return prixUnitaire;
  }

  public BigDecimal getCoutUnitaire() {
    return coutUnitaire;
  }
}

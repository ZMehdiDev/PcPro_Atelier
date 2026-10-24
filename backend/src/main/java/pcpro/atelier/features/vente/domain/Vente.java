package pcpro.atelier.features.vente.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.features.employe.domain.Employe;

/** Vente au comptoir (protection d'écran, écouteurs, PC...), payée en une fois. */
@Entity
@Table(name = "vente")
public class Vente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MoyenPaiement moyen;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "employe_id", nullable = false)
  private Employe employe;

  @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<VenteLigne> lignes = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public BigDecimal total() {
    return lignes.stream().map(VenteLigne::total).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Coût d'achat des articles dont on l'a saisi. */
  public BigDecimal cout() {
    return lignes.stream().map(VenteLigne::cout).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void ajouterLigne(VenteLigne ligne) {
    ligne.setVente(this);
    ligne.setPosition(lignes.size());
    lignes.add(ligne);
  }

  public Long getId() {
    return id;
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

  public List<VenteLigne> getLignes() {
    return lignes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

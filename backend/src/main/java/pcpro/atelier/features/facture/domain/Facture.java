package pcpro.atelier.features.facture.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.config.AtelierProperties.RegimeTva;
import pcpro.atelier.core.enums.TypeFacture;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.vente.domain.Vente;

/**
 * Facture (ou note de crédit) d'une réparation ou d'une vente. Figée à l'émission : le client, les lignes,
 * le régime de TVA et le total sont recopiés, et ne changent plus même si le dossier change ensuite.
 */
@Entity
@Table(name = "facture")
public class Facture {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TypeFacture type;

  @Column(nullable = false)
  private int annee;

  @Column(nullable = false)
  private int sequence;

  @Column(nullable = false, length = 20, unique = true)
  private String numero;

  @Column(name = "date_emission", nullable = false)
  private LocalDate dateEmission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id")
  private Dossier dossier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vente_id")
  private Vente vente;

  /** Pour une note de crédit : la facture qu'elle annule. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facture_origine_id")
  private Facture factureOrigine;

  @Column(name = "client_nom", nullable = false, length = 150)
  private String clientNom;

  @Column(name = "client_adresse", length = 300)
  private String clientAdresse;

  @Column(name = "client_tva", length = 30)
  private String clientTva;

  @Column(name = "client_email", length = 150)
  private String clientEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "regime_tva", nullable = false, length = 20)
  private RegimeTva regimeTva;

  @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
  private BigDecimal tauxTva;

  /** Total TVAC (négatif pour une note de crédit). */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "employe_id", nullable = false)
  private Employe employe;

  @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<FactureLigne> lignes = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public void ajouterLigne(FactureLigne ligne) {
    ligne.setFacture(this);
    ligne.setPosition(lignes.size());
    lignes.add(ligne);
  }

  public boolean estNoteCredit() {
    return type == TypeFacture.NOTE_CREDIT;
  }

  public Long getId() {
    return id;
  }

  public TypeFacture getType() {
    return type;
  }

  public void setType(TypeFacture type) {
    this.type = type;
  }

  public int getAnnee() {
    return annee;
  }

  public void setAnnee(int annee) {
    this.annee = annee;
  }

  public int getSequence() {
    return sequence;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public LocalDate getDateEmission() {
    return dateEmission;
  }

  public void setDateEmission(LocalDate dateEmission) {
    this.dateEmission = dateEmission;
  }

  public Dossier getDossier() {
    return dossier;
  }

  public void setDossier(Dossier dossier) {
    this.dossier = dossier;
  }

  public Vente getVente() {
    return vente;
  }

  public void setVente(Vente vente) {
    this.vente = vente;
  }

  public Facture getFactureOrigine() {
    return factureOrigine;
  }

  public void setFactureOrigine(Facture factureOrigine) {
    this.factureOrigine = factureOrigine;
  }

  public String getClientNom() {
    return clientNom;
  }

  public void setClientNom(String clientNom) {
    this.clientNom = clientNom;
  }

  public String getClientAdresse() {
    return clientAdresse;
  }

  public void setClientAdresse(String clientAdresse) {
    this.clientAdresse = clientAdresse;
  }

  public String getClientTva() {
    return clientTva;
  }

  public void setClientTva(String clientTva) {
    this.clientTva = clientTva;
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public void setClientEmail(String clientEmail) {
    this.clientEmail = clientEmail;
  }

  public RegimeTva getRegimeTva() {
    return regimeTva;
  }

  public void setRegimeTva(RegimeTva regimeTva) {
    this.regimeTva = regimeTva;
  }

  public BigDecimal getTauxTva() {
    return tauxTva;
  }

  public void setTauxTva(BigDecimal tauxTva) {
    this.tauxTva = tauxTva;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public Employe getEmploye() {
    return employe;
  }

  public void setEmploye(Employe employe) {
    this.employe = employe;
  }

  public List<FactureLigne> getLignes() {
    return lignes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

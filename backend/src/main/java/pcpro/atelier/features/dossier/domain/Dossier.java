package pcpro.atelier.features.dossier.domain;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.*;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.employe.domain.Employe;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dossier")
public class Dossier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String reference;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Enumerated(EnumType.STRING)
  @Column(name = "type_appareil", nullable = false, length = 30)
  private TypeAppareil typeAppareil;

  @Column(length = 80)
  private String marque;

  @Column(length = 120)
  private String modele;

  @Column(name = "numero_serie", length = 120)
  private String numeroSerie;

  @Column(name = "description_probleme", columnDefinition = "TEXT")
  private String descriptionProbleme;

  /** Ce que le client a laissé avec l'appareil (chargeur, sacoche...). */
  @Column(length = 255)
  private String accessoires;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private StatutDossier statut;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "technicien_assigne_id")
  private Employe technicienAssigne;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Priorite priorite = Priorite.NORMALE;

  @Column(name = "date_entree", nullable = false)
  private LocalDate dateEntree;

  @Column(name = "date_estimee")
  private LocalDate dateEstimee;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "closed_at")
  private OffsetDateTime closedAt;

  /** Clôturé = en lecture seule (hors notes) tant qu'un admin ne le rouvre pas. */
  public boolean estCloture() {
    return statut == StatutDossier.CLOTURE;
  }

  /** Date estimée dépassée alors que l'appareil n'est pas encore prêt. */
  public boolean estEnRetard(LocalDate aujourdhui) {
    return dateEstimee != null
        && dateEstimee.isBefore(aujourdhui)
        && statut != StatutDossier.PRET
        && statut != StatutDossier.CLOTURE;
  }

  public Long getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public TypeAppareil getTypeAppareil() {
    return typeAppareil;
  }

  public void setTypeAppareil(TypeAppareil typeAppareil) {
    this.typeAppareil = typeAppareil;
  }

  public String getMarque() {
    return marque;
  }

  public void setMarque(String marque) {
    this.marque = marque;
  }

  public String getModele() {
    return modele;
  }

  public void setModele(String modele) {
    this.modele = modele;
  }

  public String getNumeroSerie() {
    return numeroSerie;
  }

  public void setNumeroSerie(String numeroSerie) {
    this.numeroSerie = numeroSerie;
  }

  public String getDescriptionProbleme() {
    return descriptionProbleme;
  }

  public void setDescriptionProbleme(String descriptionProbleme) {
    this.descriptionProbleme = descriptionProbleme;
  }

  public String getAccessoires() {
    return accessoires;
  }

  public void setAccessoires(String accessoires) {
    this.accessoires = accessoires;
  }

  public StatutDossier getStatut() {
    return statut;
  }

  public void setStatut(StatutDossier statut) {
    this.statut = statut;
  }





  public Employe getTechnicienAssigne() {
    return technicienAssigne;
  }

  public void setTechnicienAssigne(Employe technicienAssigne) {
    this.technicienAssigne = technicienAssigne;
  }

  public Priorite getPriorite() {
    return priorite;
  }

  public void setPriorite(Priorite priorite) {
    this.priorite = priorite;
  }

  public LocalDate getDateEntree() {
    return dateEntree;
  }

  public void setDateEntree(LocalDate dateEntree) {
    this.dateEntree = dateEntree;
  }

  public LocalDate getDateEstimee() {
    return dateEstimee;
  }

  public void setDateEstimee(LocalDate dateEstimee) {
    this.dateEstimee = dateEstimee;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(OffsetDateTime closedAt) {
    this.closedAt = closedAt;
  }
}

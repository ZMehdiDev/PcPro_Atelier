package pcpro.atelier.features.evenement.domain;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evenement")
public class Evenement {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "dossier_id", nullable = false)
  private Dossier dossier;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "employe_id", nullable = false)
  private Employe employe;

  @Enumerated(EnumType.STRING)
  @Column(name = "type_action", nullable = false, length = 40)
  private TypeActionEvenement typeAction;

  @Column(name = "ancienne_valeur", columnDefinition = "TEXT")
  private String ancienneValeur;

  @Column(name = "nouvelle_valeur", columnDefinition = "TEXT")
  private String nouvelleValeur;

  @Column(columnDefinition = "TEXT")
  private String commentaire;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
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

  public Employe getEmploye() { 
    return employe; 
  }

  public void setEmploye(Employe employe) { 
    this.employe = employe; 
  }

  public TypeActionEvenement getTypeAction() { 
    return typeAction; 
  }

  public void setTypeAction(TypeActionEvenement typeAction) { 
    this.typeAction = typeAction; 
  }

  public String getAncienneValeur() { 
    return ancienneValeur; 
  }

  public void setAncienneValeur(String ancienneValeur) { 
    this.ancienneValeur = ancienneValeur; 
  }

  public String getNouvelleValeur() { 
    return nouvelleValeur; 
  }

  public void setNouvelleValeur(String nouvelleValeur) { 
    this.nouvelleValeur = nouvelleValeur; 
  }

  public String getCommentaire() { 
    return commentaire; 
  }

  public void setCommentaire(String commentaire) { 
    this.commentaire = commentaire; 
  }

  public OffsetDateTime getCreatedAt() { 
    return createdAt; 
  }
}

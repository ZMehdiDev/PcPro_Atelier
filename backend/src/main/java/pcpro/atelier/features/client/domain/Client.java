package pcpro.atelier.features.client.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "client", uniqueConstraints = @UniqueConstraint(name="uq_client_telephone", columnNames = "telephone"))
public class Client {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nom;

  @Column(nullable = false, length = 32)
  private String telephone;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  public Long getId() { 
    return id; 
  }

  public String getNom() { 
    return nom; 
  }

  public void setNom(String nom) { 
    this.nom = nom; 
  }

  public String getTelephone() { 
    return telephone; 
  }

  public void setTelephone(String telephone) { 
    this.telephone = telephone; 
  }

  public OffsetDateTime getCreatedAt() { 
    return createdAt; 
  }
}

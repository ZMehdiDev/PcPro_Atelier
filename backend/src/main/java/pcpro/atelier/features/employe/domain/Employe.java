package pcpro.atelier.features.employe.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import pcpro.atelier.core.enums.Role;

@Entity
@Table(name = "employe")
public class Employe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String prenom;

  /** Identifiant de connexion, toujours en minuscules. */
  @Column(nullable = false, length = 50, unique = true)
  private String login;

  /** Null tant que l'employé n'a pas de mot de passe (il ne peut alors pas se connecter). */
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.TECHNICIEN;

  @Column(nullable = false)
  private boolean actif = true;

  /** Les tokens émis avant cette date sont refusés (changement de mot de passe, désactivation). */
  @Column(name = "credentials_changed_at")
  private OffsetDateTime credentialsChangedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public Long getId() {
    return id;
  }

  public String getPrenom() {
    return prenom;
  }

  public void setPrenom(String prenom) {
    this.prenom = prenom;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public boolean isActif() {
    return actif;
  }

  public void setActif(boolean actif) {
    this.actif = actif;
  }

  public OffsetDateTime getCredentialsChangedAt() {
    return credentialsChangedAt;
  }

  public void setCredentialsChangedAt(OffsetDateTime credentialsChangedAt) {
    this.credentialsChangedAt = credentialsChangedAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

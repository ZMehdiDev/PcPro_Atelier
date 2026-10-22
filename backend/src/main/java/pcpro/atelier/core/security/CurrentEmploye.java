package pcpro.atelier.core.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import pcpro.atelier.core.enums.Role;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

/**
 * Accès à l'employé connecté (l'auteur des actions enregistrées dans l'historique).
 */
@Component
public class CurrentEmploye {

  private final EmployeRepository employeRepository;

  public CurrentEmploye(EmployeRepository employeRepository) {
    this.employeRepository = employeRepository;
  }

  public Long id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
      throw new AuthenticationCredentialsNotFoundException("Aucun employé connecté");
    }
    try {
      return Long.valueOf(auth.getName());
    } catch (NumberFormatException e) {
      throw new AuthenticationCredentialsNotFoundException("Aucun employé connecté");
    }
  }

  public Employe get() {
    return employeRepository.findById(id())
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Aucun employé connecté"));
  }

  public boolean estAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> ("ROLE_" + Role.ADMIN.name()).equals(a.getAuthority()));
  }
}

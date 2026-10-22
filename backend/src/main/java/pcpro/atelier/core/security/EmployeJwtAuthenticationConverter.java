package pcpro.atelier.core.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

/**
 * Transforme un JWT valide en authentification, en revérifiant l'employé en base à chaque requête :
 * un employé désactivé ou dont le mot de passe a changé perd immédiatement l'accès, et le rôle
 * pris en compte est toujours celui en base.
 */
@Component
public class EmployeJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final EmployeRepository employeRepository;

  public EmployeJwtAuthenticationConverter(EmployeRepository employeRepository) {
    this.employeRepository = employeRepository;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Long employeId;
    try {
      employeId = Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw new InvalidBearerTokenException("Token invalide");
    }

    Employe employe = employeRepository.findById(employeId)
        .filter(Employe::isActif)
        .orElseThrow(() -> new InvalidBearerTokenException("Employé inconnu ou désactivé"));

    Instant emisLe = jwt.getIssuedAt();
    Instant credentialsChangedAt = employe.getCredentialsChangedAt() != null
        ? employe.getCredentialsChangedAt().toInstant().truncatedTo(ChronoUnit.SECONDS)
        : null;
    if (emisLe == null || (credentialsChangedAt != null && emisLe.isBefore(credentialsChangedAt))) {
      throw new InvalidBearerTokenException("Token révoqué, reconnectez-vous");
    }

    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + employe.getRole().name()));
    return new JwtAuthenticationToken(jwt, authorities, employe.getId().toString());
  }
}

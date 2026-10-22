package pcpro.atelier.core.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import pcpro.atelier.core.config.SecurityProperties;
import pcpro.atelier.features.employe.domain.Employe;

@Service
public class JwtService {

  public record TokenEmis(String token, Instant expireLe) {
  }

  private final JwtEncoder jwtEncoder;
  private final SecurityProperties properties;
  private final Clock clock;

  public JwtService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  public TokenEmis emettre(Employe employe) {
    Instant now = clock.instant();
    Instant expireLe = now.plus(properties.jwtTtl());
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(SecurityConfig.ISSUER)
        .subject(employe.getId().toString())
        .issuedAt(now)
        .expiresAt(expireLe)
        .claim("login", employe.getLogin())
        .claim("prenom", employe.getPrenom())
        .claim("role", employe.getRole().name())
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenEmis(token, expireLe);
  }
}

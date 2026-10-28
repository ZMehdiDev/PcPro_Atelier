package pcpro.atelier.core.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration sécurité (préfixe app.security).
 *
 * @param jwtSecret            clé HMAC de signature des tokens, au moins 32 octets (APP_JWT_SECRET)
 * @param jwtTtl               durée de validité d'un token d'accès (court : il est renouvelé via le refresh token)
 * @param refreshTtl           durée d'une session "rester connecté" sans utilisation (prolongée à chaque usage)
 * @param cookieSecure         cookie de session envoyé uniquement en HTTPS (désactiver seulement en dev sur http)
 * @param loginMaxEchecs       nombre d'échecs sur un login depuis une même adresse IP avant blocage temporaire
 * @param loginMaxEchecsParIp  nombre d'échecs depuis une même adresse IP (tous logins confondus) avant blocage
 * @param loginMaxEchecsParCompte nombre d'échecs sur un login, toutes adresses IP confondues, avant blocage
 *                             (attaque répartie sur beaucoup d'adresses ; plus haut pour qu'un inconnu ne puisse
 *                             pas facilement bloquer le compte de quelqu'un)
 * @param loginBlocage         durée du blocage après trop d'échecs
 * @param corsAllowedOrigins   origines autorisées pour le front (ex : http://localhost:4200)
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    String jwtSecret,
    @DefaultValue("15m") Duration jwtTtl,
    @DefaultValue("30d") Duration refreshTtl,
    @DefaultValue("true") boolean cookieSecure,
    @DefaultValue("5") int loginMaxEchecs,
    @DefaultValue("20") int loginMaxEchecsParIp,
    @DefaultValue("50") int loginMaxEchecsParCompte,
    @DefaultValue("15m") Duration loginBlocage,
    @DefaultValue List<String> corsAllowedOrigins
) {

  public static final int JWT_SECRET_MIN_BYTES = 32;

  public SecurityProperties {
    if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < JWT_SECRET_MIN_BYTES) {
      throw new IllegalStateException(
          "app.security.jwt-secret (APP_JWT_SECRET) doit être défini et faire au moins "
              + JWT_SECRET_MIN_BYTES + " caractères");
    }
  }
}

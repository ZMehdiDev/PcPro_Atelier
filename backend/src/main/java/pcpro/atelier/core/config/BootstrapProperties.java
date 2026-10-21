package pcpro.atelier.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Compte administrateur créé (ou activé) au démarrage s'il n'a pas encore de mot de passe.
 * Permet de se connecter la toute première fois sur une base neuve.
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
    String adminLogin,
    String adminPassword,
    @DefaultValue("Admin") String adminPrenom
) {

  public boolean estConfigure() {
    return adminLogin != null && !adminLogin.isBlank()
        && adminPassword != null && !adminPassword.isBlank();
  }
}

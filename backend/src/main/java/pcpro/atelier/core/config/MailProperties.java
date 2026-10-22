package pcpro.atelier.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Envoi des emails aux clients. Le serveur SMTP se règle avec spring.mail.* (SPRING_MAIL_HOST...) ;
 * sans serveur configuré, l'appli fonctionne normalement et n'envoie simplement pas d'email.
 *
 * @param expediteur adresse d'envoi, ex : "Pc Pro &lt;atelier@pcpro-services.be&gt;" (domaine validé chez le fournisseur)
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(@DefaultValue("") String expediteur) {
}

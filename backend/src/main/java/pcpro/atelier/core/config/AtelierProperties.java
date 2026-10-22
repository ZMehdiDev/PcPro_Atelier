package pcpro.atelier.core.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Informations de l'atelier (bon de dépôt, emails, suivi en ligne) et règles métier réglables.
 *
 * @param nom                  nom affiché aux clients
 * @param adresse              adresse imprimée sur le bon de dépôt et dans les emails
 * @param telephone            téléphone de l'atelier
 * @param email                email de contact de l'atelier
 * @param horaires             horaires d'ouverture (ex : "Lun-Ven 9h-18h, Sam 10h-16h")
 * @param urlPublique          adresse de l'appli (ex : https://atelier.pcpro-services.be), pour les liens de suivi
 * @param garantieMois         garantie par défaut sur une réparation, en mois
 * @param delaiRecuperation    au-delà, un appareil prêt et pas récupéré est signalé
 * @param conditionsDepot      conditions imprimées en bas du bon de dépôt
 */
@ConfigurationProperties(prefix = "app.atelier")
public record AtelierProperties(
    @DefaultValue("Pc Pro") String nom,
    @DefaultValue("") String adresse,
    @DefaultValue("") String telephone,
    @DefaultValue("") String email,
    @DefaultValue("") String horaires,
    @DefaultValue("") String urlPublique,
    @DefaultValue("3") int garantieMois,
    @DefaultValue("7d") Duration delaiRecuperation,
    @DefaultValue("") String conditionsDepot
) {

  public AtelierProperties {
    urlPublique = urlPublique.endsWith("/") ? urlPublique.substring(0, urlPublique.length() - 1) : urlPublique;
  }

  /** Lien de suivi d'un dossier, ou null si l'adresse publique de l'appli n'est pas configurée. */
  public String lienSuivi(String reference, String codeSuivi) {
    return urlPublique.isBlank() ? null : urlPublique + "/suivi/" + reference + "?code=" + codeSuivi;
  }
}

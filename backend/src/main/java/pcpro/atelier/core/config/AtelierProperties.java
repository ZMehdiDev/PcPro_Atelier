package pcpro.atelier.core.config;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Informations de l'atelier (documents imprimés, emails, suivi en ligne) et règles métier réglables.
 *
 * @param nom                  nom commercial affiché aux clients
 * @param slogan               une ligne sous le nom sur les documents (ex : "Réparation informatique")
 * @param raisonSociale        nom légal de l'entreprise (personne physique ou société), imprimé sur les factures
 * @param bce                  numéro d'entreprise BCE (10 chiffres, points facultatifs)
 * @param tva                  régime de TVA appliqué aux factures
 * @param iban                 compte bancaire imprimé sur les factures (virement, QR code de paiement)
 * @param bic                  BIC de la banque (facultatif)
 * @param adresse              adresse imprimée sur les documents et dans les emails
 * @param telephone            téléphone de l'atelier
 * @param email                email de contact de l'atelier
 * @param siteWeb              site vitrine (ex : pcpro-services.be)
 * @param horaires             horaires d'ouverture (ex : "Lun-Ven 9h-18h, Sam 10h-16h")
 * @param logo                 image (PNG, JPG) remplaçant le logo dessiné en haut des documents, facultatif
 * @param urlPublique          adresse de l'appli (ex : https://atelier.pcpro-services.be), pour les liens de suivi
 * @param garantieMois         garantie par défaut sur une réparation, en mois
 * @param delaiRecuperation    au-delà, un appareil prêt et pas récupéré est signalé
 * @param devisValiditeJours   durée pendant laquelle un devis engage l'atelier
 * @param conditionsDepot      conditions imprimées sur le bon de dépôt (vide : conditions par défaut), une par ligne
 * @param mentionFacture       texte libre en bas des factures (conditions de paiement...)
 */
@ConfigurationProperties(prefix = "app.atelier")
public record AtelierProperties(
    @DefaultValue("Pc Pro") String nom,
    @DefaultValue("") String slogan,
    @DefaultValue("") String raisonSociale,
    @DefaultValue("") String bce,
    @DefaultValue Tva tva,
    @DefaultValue("") String iban,
    @DefaultValue("") String bic,
    @DefaultValue("") String adresse,
    @DefaultValue("") String telephone,
    @DefaultValue("") String email,
    @DefaultValue("") String siteWeb,
    @DefaultValue("") String horaires,
    @DefaultValue("") String logo,
    @DefaultValue("") String urlPublique,
    @DefaultValue("3") int garantieMois,
    @DefaultValue("7d") Duration delaiRecuperation,
    @DefaultValue("30") int devisValiditeJours,
    @DefaultValue("") String conditionsDepot,
    @DefaultValue("") String mentionFacture
) {

  public AtelierProperties {
    urlPublique = urlPublique.endsWith("/") ? urlPublique.substring(0, urlPublique.length() - 1) : urlPublique;
    bce = bce.replaceAll("[^0-9]", "");
    iban = iban.replaceAll("\\s", "").toUpperCase();
    bic = bic.replaceAll("\\s", "").toUpperCase();
  }

  /**
   * TVA. En franchise (petite entreprise), aucune TVA n'est facturée et une mention légale est imprimée.
   * Assujetti, les prix saisis dans l'appli sont TVAC et les factures détaillent HTVA, TVA et TVAC.
   *
   * @param regime FRANCHISE ou ASSUJETTI
   * @param taux   taux normal en pourcent (21 en Belgique)
   */
  public record Tva(
      @DefaultValue("FRANCHISE") RegimeTva regime,
      @DefaultValue("21") BigDecimal taux
  ) {
  }

  public enum RegimeTva {
    FRANCHISE,
    ASSUJETTI
  }

  /** Lien de suivi d'un dossier, ou null si l'adresse publique de l'appli n'est pas configurée. */
  public String lienSuivi(String reference, String codeSuivi) {
    return urlPublique.isBlank() ? null : urlPublique + "/suivi/" + reference + "?code=" + codeSuivi;
  }

  /** Lien vers la fiche d'un dossier dans l'appli (étiquette collée sur l'appareil), ou null. */
  public String lienDossier(Long dossierId) {
    return urlPublique.isBlank() ? null : urlPublique + "/dossiers/" + dossierId;
  }

  /** Ex : "0895.968.808", ou "" si non configuré. */
  public String bceFormate() {
    return bce.length() == 10 ? bce.substring(0, 4) + "." + bce.substring(4, 7) + "." + bce.substring(7) : bce;
  }

  /** Numéro de TVA belge (BE + numéro d'entreprise), ou "" si non configuré. */
  public String numeroTva() {
    return bce.isBlank() ? "" : "BE " + bceFormate();
  }

  public boolean assujettiTva() {
    return tva.regime() == RegimeTva.ASSUJETTI;
  }
}

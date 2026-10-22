package pcpro.atelier.features.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.domain.DevisLigne;
import pcpro.atelier.features.dossier.domain.Dossier;

/**
 * Contenu des emails envoyés aux clients. Sobre : quelques paragraphes, un lien de suivi, les coordonnées.
 */
@Component
public class EmailTemplates {

  private final AtelierProperties atelier;

  public EmailTemplates(AtelierProperties atelier) {
    this.atelier = atelier;
  }

  /** L'appareil est réparé (ou rendu) : le client peut venir le chercher. */
  public Email pret(Dossier dossier, BigDecimal resteAPayer) {
    List<String> paragraphes = new ArrayList<>();
    paragraphes.add("Bonne nouvelle : votre " + appareil(dossier) + " (dossier " + dossier.getReference()
        + ") est prêt. Vous pouvez venir le récupérer" + horaires() + ".");
    if (resteAPayer != null && resteAPayer.signum() > 0) {
      paragraphes.add("Montant à régler : " + Libelles.montant(resteAPayer) + ".");
    }
    return email(dossier, "Votre " + appareil(dossier) + " est prêt", paragraphes, List.of());
  }

  /** Rappel pour un appareil prêt que personne n'est venu chercher. */
  public Email relance(Dossier dossier) {
    String depuis = dossier.getPretAt() != null
        ? " depuis le " + Libelles.DATE.format(dossier.getPretAt().toLocalDate())
        : "";
    List<String> paragraphes = List.of(
        "Votre " + appareil(dossier) + " (dossier " + dossier.getReference() + ") est prêt" + depuis
            + " et vous attend à l'atelier.",
        "Vous pouvez venir le récupérer" + horaires() + ". Si vous avez un empêchement, n'hésitez pas à nous appeler."
    );
    return email(dossier, "Votre " + appareil(dossier) + " vous attend", paragraphes, List.of());
  }

  /** Devis à accepter ou refuser (en ligne via le lien de suivi, ou par téléphone). */
  public Email devis(Dossier dossier, Devis devis) {
    List<String> paragraphes = new ArrayList<>();
    paragraphes.add("Voici notre devis pour la réparation de votre " + appareil(dossier)
        + " (dossier " + dossier.getReference() + ").");
    if (devis.getCommentaire() != null) {
      paragraphes.add(devis.getCommentaire());
    }
    paragraphes.add("Total TTC : " + Libelles.montant(devis.total()) + ".");
    paragraphes.add(lienSuivi(dossier) != null
        ? "Vous pouvez l'accepter ou le refuser en ligne avec le lien ci-dessous, ou nous répondre par téléphone."
        : "Merci de nous donner votre réponse par téléphone ou par retour d'email.");

    List<String[]> tableau = new ArrayList<>();
    for (DevisLigne ligne : devis.getLignes()) {
      String quantite = ligne.getQuantite().stripTrailingZeros().toPlainString().replace('.', ',');
      tableau.add(new String[] {
          ligne.getLibelle() + ("1".equals(quantite) ? "" : " × " + quantite),
          Libelles.montant(ligne.total())
      });
    }
    return email(dossier, "Devis pour votre " + appareil(dossier), paragraphes, tableau);
  }

  // ---------- Mise en forme ----------

  private Email email(Dossier dossier, String sujet, List<String> paragraphes, List<String[]> tableau) {
    String lien = lienSuivi(dossier);
    String salutation = "Bonjour " + dossier.getClient().getNom() + ",";

    StringBuilder texte = new StringBuilder(salutation).append("\n\n");
    paragraphes.forEach(p -> texte.append(p).append("\n\n"));
    tableau.forEach(l -> texte.append("- ").append(l[0]).append(" : ").append(l[1]).append('\n'));
    if (!tableau.isEmpty()) {
      texte.append('\n');
    }
    if (lien != null) {
      texte.append("Suivre votre dossier : ").append(lien).append("\n\n");
    }
    texte.append("À bientôt,\n").append(signatureTexte());

    StringBuilder html = new StringBuilder()
        .append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.5;color:#222;max-width:560px\">")
        .append("<p>").append(e(salutation)).append("</p>");
    paragraphes.forEach(p -> html.append("<p>").append(e(p)).append("</p>"));
    if (!tableau.isEmpty()) {
      html.append("<table style=\"border-collapse:collapse;width:100%;margin:8px 0 16px\">");
      tableau.forEach(l -> html
          .append("<tr><td style=\"padding:6px 0;border-bottom:1px solid #ddd\">").append(e(l[0]))
          .append("</td><td style=\"padding:6px 0;border-bottom:1px solid #ddd;text-align:right;white-space:nowrap\">")
          .append(e(l[1])).append("</td></tr>"));
      html.append("</table>");
    }
    if (lien != null) {
      html.append("<p><a href=\"").append(e(lien)).append("\" style=\"color:#1a56a8\">Suivre votre dossier en ligne</a></p>");
    }
    html.append("<p>À bientôt,<br>").append(e(signatureTexte()).replace("\n", "<br>")).append("</p></div>");

    return new Email(dossier.getClient().getEmail(), sujet + " - " + atelier.nom(), texte.toString(), html.toString());
  }

  private String signatureTexte() {
    StringBuilder signature = new StringBuilder(atelier.nom());
    for (String ligne : new String[] {atelier.adresse(), atelier.telephone()}) {
      if (!ligne.isBlank()) {
        signature.append('\n').append(ligne);
      }
    }
    return signature.toString();
  }

  private String horaires() {
    return atelier.horaires().isBlank() ? " aux heures d'ouverture" : " (" + atelier.horaires() + ")";
  }

  private String lienSuivi(Dossier dossier) {
    return atelier.lienSuivi(dossier.getReference(), dossier.getCodeSuivi());
  }

  private static String appareil(Dossier dossier) {
    return Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele());
  }

  private static String e(String texte) {
    return HtmlUtils.htmlEscape(texte);
  }
}

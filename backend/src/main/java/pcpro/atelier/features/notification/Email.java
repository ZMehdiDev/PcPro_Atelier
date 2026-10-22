package pcpro.atelier.features.notification;

/** Email prêt à partir : version texte et version HTML du même contenu. */
public record Email(String destinataire, String sujet, String texte, String html) {
}

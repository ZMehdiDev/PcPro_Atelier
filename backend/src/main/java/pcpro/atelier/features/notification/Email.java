package pcpro.atelier.features.notification;

/** Email prêt à partir : version texte et version HTML du même contenu, et éventuellement un PDF joint. */
public record Email(String destinataire, String sujet, String texte, String html, PieceJointe pieceJointe) {

  public record PieceJointe(String nomFichier, byte[] pdf) {
  }

  public Email(String destinataire, String sujet, String texte, String html) {
    this(destinataire, sujet, texte, html, null);
  }

  public Email avecPieceJointe(String nomFichier, byte[] pdf) {
    return new Email(destinataire, sujet, texte, html, new PieceJointe(nomFichier, pdf));
  }
}

package pcpro.atelier.core.exception;

/**
 * Requête invalide (donnée incohérente, fichier refusé...). Renvoie un 400.
 */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}

package pcpro.atelier.core.exception;

/**
 * Action impossible dans l'état actuel (transition interdite, doublon...). Renvoie un 409.
 */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}

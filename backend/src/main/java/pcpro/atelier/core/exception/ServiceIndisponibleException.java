package pcpro.atelier.core.exception;

/** Un service externe (envoi d'emails) n'est pas configuré ou ne répond pas. */
public class ServiceIndisponibleException extends RuntimeException {

  public ServiceIndisponibleException(String message) {
    super(message);
  }
}

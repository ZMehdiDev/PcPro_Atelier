package pcpro.atelier.core.exception;

public class MissingHeaderException extends RuntimeException {
  
  public MissingHeaderException(String headerName) {
    super(String.format("Le header '%s' est obligatoire", headerName));
  }
}

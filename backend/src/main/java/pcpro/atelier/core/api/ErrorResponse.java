package pcpro.atelier.core.api;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.MDC;

import pcpro.atelier.core.api.filter.RequestIdFilter;

/**
 * Format unique des erreurs renvoyées par l'API.
 */
public record ErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    String traceId,
    List<FieldError> fieldErrors
) {

  public record FieldError(String field, String message) {
  }

  public static ErrorResponse of(int status, String error, String message, String path) {
    return of(status, error, message, path, List.of());
  }

  public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
    return new ErrorResponse(
        OffsetDateTime.now(),
        status,
        error,
        message,
        path,
        MDC.get(RequestIdFilter.MDC_REQUEST_ID),
        fieldErrors
    );
  }
}

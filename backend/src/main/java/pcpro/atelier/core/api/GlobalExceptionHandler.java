package pcpro.atelier.core.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.exception.ServiceIndisponibleException;
import pcpro.atelier.core.exception.TooManyRequestsException;

/**
 * Traduit toutes les exceptions en {@link ErrorResponse}. Les messages techniques
 * (SQL, chemins de fichiers, stack traces) ne sont jamais renvoyés au client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
    logger.debug("Conflit sur {}: {}", request.getRequestURI(), ex.getMessage());
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
    return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
  }

  @ExceptionHandler(ServiceIndisponibleException.class)
  public ResponseEntity<ErrorResponse> handleServiceIndisponible(ServiceIndisponibleException ex, HttpServletRequest request) {
    return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
    String message = ex instanceof BadCredentialsException ? ex.getMessage() : "Authentification requise";
    return build(HttpStatus.UNAUTHORIZED, message, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Action réservée aux administrateurs", request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
    List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
        .map(cv -> new ErrorResponse.FieldError(String.valueOf(cv.getPropertyPath()), cv.getMessage()))
        .toList();
    return validationFailed(fieldErrors, request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Valeur invalide pour le paramètre '" + ex.getName() + "'", request);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Tri invalide : propriété '" + ex.getPropertyName() + "' inconnue", request);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, "La ressource a été modifiée en même temps par quelqu'un d'autre, réessayez", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    logger.warn("Violation d'intégrité sur {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
    return build(HttpStatus.CONFLICT, "Opération refusée : données en conflit avec l'existant", request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    logger.error("Erreur inattendue sur {}", request.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue s'est produite", request);
  }

  // ---------- Exceptions Spring MVC standard ----------

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();
    return new ResponseEntity<>(validationFailed(fieldErrors, path(request)).getBody(), HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    List<ErrorResponse.FieldError> fieldErrors = ex.getParameterValidationResults().stream()
        .flatMap(result -> result.getResolvableErrors().stream()
            .map(error -> new ErrorResponse.FieldError(
                result.getMethodParameter().getParameterName(),
                error.getDefaultMessage())))
        .toList();
    return new ResponseEntity<>(validationFailed(fieldErrors, path(request)).getBody(), HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    return bodyOf(HttpStatus.BAD_REQUEST, "Corps de requête illisible ou mal formé", request);
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    return bodyOf(HttpStatus.CONTENT_TOO_LARGE, "Fichier trop volumineux", request);
  }

  /**
   * Toutes les autres exceptions MVC (404, 405, 415, paramètre manquant...) gardent leur statut
   * mais avec notre format de réponse.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request
  ) {
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    String message = switch (statusCode.value()) {
      case 404 -> "Ressource introuvable";
      case 405 -> "Méthode HTTP non supportée";
      case 415 -> "Type de contenu non supporté";
      case 400 -> "Requête invalide : " + (ex.getMessage() != null ? ex.getMessage() : "paramètre manquant");
      default -> status != null ? status.getReasonPhrase() : "Erreur";
    };
    ErrorResponse error = ErrorResponse.of(
        statusCode.value(),
        status != null ? status.getReasonPhrase() : "Error",
        message,
        path(request)
    );
    return new ResponseEntity<>(error, headers, statusCode);
  }

  private ResponseEntity<Object> bodyOf(HttpStatus status, String message, WebRequest request) {
    return new ResponseEntity<>(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path(request)), status);
  }

  private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
  }

  private static ResponseEntity<ErrorResponse> validationFailed(List<ErrorResponse.FieldError> fieldErrors, String path) {
    HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(status.value(), "Validation Failed", "Erreurs de validation", path, fieldErrors));
  }

  private static String path(WebRequest request) {
    return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
  }
}

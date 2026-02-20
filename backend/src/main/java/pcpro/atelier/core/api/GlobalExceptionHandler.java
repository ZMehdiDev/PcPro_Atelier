package pcpro.atelier.core.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.MissingHeaderException;
import pcpro.atelier.core.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MDC_REQUEST_ID = "requestId";

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request
  ) {
    logger.debug("Ressource non trouvée sur {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error = buildErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        ex.getMessage(),
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      ConflictException ex,
      HttpServletRequest request
  ) {
    logger.warn("Conflit sur {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error = buildErrorResponse(
        HttpStatus.CONFLICT.value(),
        "Conflict",
        ex.getMessage(),
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
        .collect(Collectors.toList());
    
    String message = fieldErrors.stream()
        .map(e -> e.field() + ": " + e.message())
        .collect(Collectors.joining(", "));
    
    logger.debug("Erreur de validation sur {}: {}", request.getRequestURI(), message);
    ErrorResponse error = buildErrorResponse(
        HttpStatus.UNPROCESSABLE_ENTITY.value(), // 422 pour validation
        "Validation Failed",
        "Erreurs de validation",
        request,
        fieldErrors
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request
  ) {
    List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
        .map(cv -> {
          String field = cv.getPropertyPath() != null ? 
              cv.getPropertyPath().toString() : "unknown";
          return new FieldError(field, cv.getMessage());
        })
        .collect(Collectors.toList());
    
    String message = fieldErrors.stream()
        .map(e -> e.field() + ": " + e.message())
        .collect(Collectors.joining(", "));
    
    ErrorResponse error = buildErrorResponse(
        HttpStatus.UNPROCESSABLE_ENTITY.value(), // 422 pour validation
        "Validation Failed",
        "Erreurs de validation",
        request,
        fieldErrors
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
  }

  @ExceptionHandler(MissingHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(
      MissingHeaderException ex,
      HttpServletRequest request
  ) {
    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        ex.getMessage(),
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException ex,
      HttpServletRequest request
  ) {
    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        "Header manquant: " + ex.getHeaderName(),
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        ex.getMessage(),
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex,
      HttpServletRequest request
  ) {
    logger.error("Erreur inattendue sur {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    ErrorResponse error = buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        "Une erreur inattendue s'est produite",
        request,
        null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Construit une ErrorResponse avec traceId depuis le MDC.
   */
  private ErrorResponse buildErrorResponse(
      int status,
      String error,
      String message,
      HttpServletRequest request,
      List<FieldError> fieldErrors
  ) {
    String traceId = MDC.get(MDC_REQUEST_ID);
    return new ErrorResponse(
        OffsetDateTime.now(),
        status,
        error,
        message,
        request.getRequestURI(),
        traceId,
        fieldErrors != null ? fieldErrors : List.of()
    );
  }

  public record ErrorResponse(
      OffsetDateTime timestamp,
      int status,
      String error,
      String message,
      String path,
      String traceId,
      List<FieldError> fieldErrors
  ) {}

  public record FieldError(
      String field,
      String message
  ) {}
}

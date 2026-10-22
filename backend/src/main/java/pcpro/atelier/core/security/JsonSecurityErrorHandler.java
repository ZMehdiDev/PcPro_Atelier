package pcpro.atelier.core.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pcpro.atelier.core.api.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Renvoie les 401/403 de la couche sécurité au même format JSON que le reste de l'API.
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    write(response, request, HttpStatus.UNAUTHORIZED, "Authentification requise ou token invalide/expiré");
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(response, request, HttpStatus.FORBIDDEN, "Accès refusé");
  }

  private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getOutputStream(),
        ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
  }
}

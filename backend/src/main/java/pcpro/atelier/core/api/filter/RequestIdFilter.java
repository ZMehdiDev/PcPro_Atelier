package pcpro.atelier.core.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre pour générer/capturer le correlation-id (X-Request-Id).
 * Si absent dans les headers, génère un UUID et l'ajoute au MDC pour les logs.
 */
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String MDC_REQUEST_ID = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    
    // Ajouter au MDC pour les logs
    MDC.put(MDC_REQUEST_ID, requestId);
    
    // Ajouter dans la réponse pour que le client puisse le récupérer
    response.setHeader(REQUEST_ID_HEADER, requestId);
    
    // Stocker dans l'attribut de la requête pour accès dans les handlers
    request.setAttribute(MDC_REQUEST_ID, requestId);
    
    try {
      filterChain.doFilter(request, response);
    } finally {
      // Nettoyer le MDC après la requête
      MDC.remove(MDC_REQUEST_ID);
    }
  }
}

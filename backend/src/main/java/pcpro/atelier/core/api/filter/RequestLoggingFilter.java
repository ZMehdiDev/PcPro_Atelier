package pcpro.atelier.core.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Filtre pour logger toutes les requêtes HTTP avec les informations structurées :
 * method, path, status, duration, requestId, employeId
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private static final String MDC_REQUEST_ID = "requestId";
  private static final String MDC_EMPLOYE_ID = "employeId";
  private static final String EMPLOYE_ID_HEADER = "X-Employe-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    
    // Ignorer les requêtes vers Swagger/actuator pour éviter le spam
    String path = request.getRequestURI();
    if (path.startsWith("/swagger-ui") || 
        path.startsWith("/v3/api-docs") || 
        path.startsWith("/actuator")) {
      filterChain.doFilter(request, response);
      return;
    }
    
    Instant startTime = Instant.now();
    String method = request.getMethod();
    String requestId = (String) request.getAttribute(MDC_REQUEST_ID);
    String employeId = request.getHeader(EMPLOYE_ID_HEADER);
    
    // Ajouter employeId au MDC si présent
    if (employeId != null && !employeId.isBlank()) {
      MDC.put(MDC_EMPLOYE_ID, employeId);
    }
    
    int status = HttpServletResponse.SC_OK;
    
    try {
      filterChain.doFilter(request, response);
      status = response.getStatus();
    } finally {
      Instant endTime = Instant.now();
      long durationMs = Duration.between(startTime, endTime).toMillis();
      
      // Logger avec toutes les infos structurées
      // Format: method path status durationMs requestId=xxx employeId=xxx
      logger.info("{} {} {} {}ms requestId={} employeId={}", 
          method, 
          path, 
          status, 
          durationMs,
          requestId != null ? requestId : "N/A",
          employeId != null ? employeId : "N/A"
      );
      
      // Nettoyer le MDC
      MDC.remove(MDC_EMPLOYE_ID);
    }
  }
}

package pcpro.atelier.core.api.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Génère/capture le correlation-id (X-Request-Id) et le place dans le MDC pour les logs.
 * Passe avant la sécurité pour que les erreurs 401/403 aient aussi un traceId.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_REQUEST_ID = "requestId";

  // Un id fourni par le client n'est repris que s'il est court et sans caractères exotiques (log injection)
  private static final Pattern REQUEST_ID_VALIDE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || !REQUEST_ID_VALIDE.matcher(requestId).matches()) {
      requestId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_REQUEST_ID, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    request.setAttribute(MDC_REQUEST_ID, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID);
    }
  }
}

package pcpro.atelier.core.api.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pcpro.atelier.core.security.EmployeMdcFilter;

/**
 * Log une ligne par requête HTTP : method path status durationMs requestId employeId.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Ignorer Swagger pour éviter le spam
    String path = request.getRequestURI();
    return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    long start = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - start) / 1_000_000;
      Object employeId = request.getAttribute(EmployeMdcFilter.ATTRIBUTE_EMPLOYE_ID);
      logger.info("{} {} {} {}ms requestId={} employeId={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs,
          request.getAttribute(RequestIdFilter.MDC_REQUEST_ID),
          employeId != null ? employeId : "N/A"
      );
    }
  }
}

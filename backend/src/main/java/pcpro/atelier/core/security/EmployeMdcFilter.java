package pcpro.atelier.core.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Met l'id de l'employé authentifié dans le MDC et dans un attribut de requête (lu par le log d'accès).
 */
public class EmployeMdcFilter extends OncePerRequestFilter {

  public static final String ATTRIBUTE_EMPLOYE_ID = "employeId";
  private static final String MDC_EMPLOYE_ID = "employeId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken) {
      request.setAttribute(ATTRIBUTE_EMPLOYE_ID, auth.getName());
      MDC.put(MDC_EMPLOYE_ID, auth.getName());
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_EMPLOYE_ID);
    }
  }
}

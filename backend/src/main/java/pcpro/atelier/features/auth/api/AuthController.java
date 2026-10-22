package pcpro.atelier.features.auth.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pcpro.atelier.core.config.SecurityProperties;
import pcpro.atelier.features.auth.api.dto.ChangeMotDePasseRequest;
import pcpro.atelier.features.auth.api.dto.LoginRequest;
import pcpro.atelier.features.auth.api.dto.LoginResponse;
import pcpro.atelier.features.auth.app.AuthService;
import pcpro.atelier.features.employe.api.dto.EmployeDto;

/**
 * Connexion.
 *
 * Le token d'accès (JSON, durée courte) va dans le header Authorization. Le refresh token est un
 * cookie HttpOnly limité à /api/auth : il permet de rester connecté des semaines sur un téléphone
 * sans que le JavaScript puisse le lire.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  public static final String REFRESH_COOKIE = "atelier_refresh";
  private static final String REFRESH_COOKIE_PATH = "/api/auth";

  private final AuthService authService;
  private final SecurityProperties properties;

  public AuthController(AuthService authService, SecurityProperties properties) {
    this.authService = authService;
    this.properties = properties;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    AuthService.Connexion connexion = authService.login(
        request.login(), request.motDePasse(), http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    return avecCookie(connexion);
  }

  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
      HttpServletRequest http
  ) {
    return avecCookie(authService.renouveler(refreshToken, http.getHeader(HttpHeaders.USER_AGENT)));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
    authService.deconnecter(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", 0).toString())
        .build();
  }

  @GetMapping("/moi")
  public EmployeDto moi() {
    return authService.moi();
  }

  @PostMapping("/mot-de-passe")
  public ResponseEntity<LoginResponse> changerMotDePasse(
      @Valid @RequestBody ChangeMotDePasseRequest request,
      HttpServletRequest http
  ) {
    return avecCookie(authService.changerMotDePasse(
        request.ancienMotDePasse(), request.nouveauMotDePasse(), http.getHeader(HttpHeaders.USER_AGENT)));
  }

  private ResponseEntity<LoginResponse> avecCookie(AuthService.Connexion connexion) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(connexion.refreshToken(), properties.refreshTtl().toSeconds()).toString())
        .body(connexion.reponse());
  }

  private ResponseCookie cookie(String valeur, long maxAgeSecondes) {
    return ResponseCookie.from(REFRESH_COOKIE, valeur)
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Strict")
        .path(REFRESH_COOKIE_PATH)
        .maxAge(maxAgeSecondes)
        .build();
  }
}

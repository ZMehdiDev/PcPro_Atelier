package pcpro.atelier.features.auth.api;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.auth.api.dto.ChangeMotDePasseRequest;
import pcpro.atelier.features.auth.api.dto.LoginRequest;
import pcpro.atelier.features.auth.api.dto.LoginResponse;
import pcpro.atelier.features.auth.app.AuthService;
import pcpro.atelier.features.employe.api.dto.EmployeDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.login(), request.motDePasse());
  }

  @GetMapping("/moi")
  public EmployeDto moi() {
    return authService.moi();
  }

  @PostMapping("/mot-de-passe")
  public LoginResponse changerMotDePasse(@Valid @RequestBody ChangeMotDePasseRequest request) {
    return authService.changerMotDePasse(request.ancienMotDePasse(), request.nouveauMotDePasse());
  }
}

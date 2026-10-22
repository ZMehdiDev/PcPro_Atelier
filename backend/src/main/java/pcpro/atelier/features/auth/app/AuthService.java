package pcpro.atelier.features.auth.app;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.TooManyRequestsException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.core.security.JwtService;
import pcpro.atelier.core.security.LoginAttemptService;
import pcpro.atelier.features.auth.api.dto.LoginResponse;
import pcpro.atelier.features.employe.api.dto.EmployeDto;
import pcpro.atelier.features.employe.app.EmployeService;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

@Service
@Transactional
public class AuthService {

  private final EmployeRepository employeRepository;
  private final EmployeService employeService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginAttemptService loginAttemptService;
  private final CurrentEmploye currentEmploye;

  // Hash factice : un login inconnu coûte le même temps qu'un mauvais mot de passe (pas d'énumération)
  private final String hashFactice;

  public AuthService(
      EmployeRepository employeRepository,
      EmployeService employeService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginAttemptService loginAttemptService,
      CurrentEmploye currentEmploye
  ) {
    this.employeRepository = employeRepository;
    this.employeService = employeService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginAttemptService = loginAttemptService;
    this.currentEmploye = currentEmploye;
    this.hashFactice = passwordEncoder.encode("mot-de-passe-factice");
  }

  @Transactional(readOnly = true)
  public LoginResponse login(String loginSaisi, String motDePasse) {
    String login = EmployeService.normaliserLogin(loginSaisi);
    if (loginAttemptService.estBloque(login)) {
      throw new TooManyRequestsException("Trop de tentatives échouées, réessayez dans quelques minutes");
    }

    Employe employe = employeRepository.findByLogin(login).orElse(null);
    boolean ok = employe != null
        && employe.getPasswordHash() != null
        && verifier(motDePasse, employe.getPasswordHash());
    if (employe == null || employe.getPasswordHash() == null) {
      verifier(motDePasse, hashFactice);
    }

    if (!ok || !employe.isActif()) {
      loginAttemptService.echec(login);
      throw new BadCredentialsException("Login ou mot de passe incorrect");
    }

    loginAttemptService.succes(login);
    return reponse(employe);
  }

  @Transactional(readOnly = true)
  public EmployeDto moi() {
    return EmployeService.toDto(currentEmploye.get());
  }

  /**
   * Change son propre mot de passe. Les autres sessions sont déconnectées, un nouveau token est renvoyé.
   */
  public LoginResponse changerMotDePasse(String ancien, String nouveau) {
    Employe employe = currentEmploye.get();
    if (!verifier(ancien, employe.getPasswordHash())) {
      throw new BadRequestException("L'ancien mot de passe est incorrect");
    }
    employeService.definirMotDePasse(employe, nouveau);
    employeRepository.flush();
    return reponse(employe);
  }

  private boolean verifier(String motDePasse, String hash) {
    try {
      return passwordEncoder.matches(motDePasse, hash);
    } catch (IllegalArgumentException e) {
      return false; // mot de passe trop long pour bcrypt, hash illisible...
    }
  }

  private LoginResponse reponse(Employe employe) {
    JwtService.TokenEmis token = jwtService.emettre(employe);
    return new LoginResponse(token.token(), token.expireLe(), EmployeService.toDto(employe));
  }
}

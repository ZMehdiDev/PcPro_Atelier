package pcpro.atelier.features.auth.app;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  /**
   * Résultat d'une connexion : le token d'accès (renvoyé en JSON) et le refresh token
   * (posé en cookie HttpOnly par le contrôleur, jamais exposé au JavaScript).
   */
  public record Connexion(LoginResponse reponse, String refreshToken) {
  }

  private final EmployeRepository employeRepository;
  private final EmployeService employeService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SessionService sessionService;
  private final LoginAttemptService loginAttemptService;
  private final CurrentEmploye currentEmploye;
  private final Clock clock;

  // Hash factice : un login inconnu coûte le même temps qu'un mauvais mot de passe (pas d'énumération)
  private final String hashFactice;

  public AuthService(
      EmployeRepository employeRepository,
      EmployeService employeService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      SessionService sessionService,
      LoginAttemptService loginAttemptService,
      CurrentEmploye currentEmploye,
      Clock clock
  ) {
    this.employeRepository = employeRepository;
    this.employeService = employeService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.sessionService = sessionService;
    this.loginAttemptService = loginAttemptService;
    this.currentEmploye = currentEmploye;
    this.clock = clock;
    this.hashFactice = passwordEncoder.encode("mot-de-passe-factice");
  }

  public Connexion login(String loginSaisi, String motDePasse, String ip, String userAgent) {
    String login = EmployeService.normaliserLogin(loginSaisi);
    if (loginAttemptService.estBloque(login, ip)) {
      log.warn("Connexion refusée (bloquée) : login={} ip={}", LoginAttemptService.nettoyer(login), ip);
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
      loginAttemptService.echec(login, ip);
      log.warn("Échec de connexion : login={} ip={}", LoginAttemptService.nettoyer(login), ip);
      throw new BadCredentialsException("Login ou mot de passe incorrect");
    }

    loginAttemptService.succes(login, ip);
    log.info("Connexion : employé {} ({}) depuis {}", employe.getId(), employe.getLogin(), ip);
    return connexion(employe, userAgent);
  }

  /**
   * Nouveau token d'accès à partir du refresh token (le front l'appelle quand le token d'accès expire).
   */
  @Transactional(noRollbackFor = BadCredentialsException.class)
  public Connexion renouveler(String refreshToken, String userAgent) {
    SessionService.SessionOuverte session = sessionService.renouveler(refreshToken, userAgent);
    return new Connexion(reponse(session.employe()), session.refreshToken());
  }

  public void deconnecter(String refreshToken) {
    sessionService.fermer(refreshToken);
  }

  @Transactional(readOnly = true)
  public EmployeDto moi() {
    return EmployeService.toDto(currentEmploye.get());
  }

  /**
   * Change son propre mot de passe. Tous les appareils sont déconnectés, sauf celui-ci qui reçoit
   * une nouvelle session.
   */
  public Connexion changerMotDePasse(String ancien, String nouveau, String ip, String userAgent) {
    Employe employe = currentEmploye.get();
    // Quelqu'un qui a pris un téléphone resté connecté ne doit pas pouvoir deviner le mot de passe ici
    if (loginAttemptService.estBloque(employe.getLogin(), ip)) {
      throw new TooManyRequestsException("Trop de tentatives échouées, réessayez dans quelques minutes");
    }
    if (!verifier(ancien, employe.getPasswordHash())) {
      loginAttemptService.echec(employe.getLogin(), ip);
      throw new BadRequestException("L'ancien mot de passe est incorrect");
    }
    employeService.definirMotDePasse(employe, nouveau);
    employeRepository.flush();
    log.info("Mot de passe changé par l'employé {}", employe.getId());
    return connexion(employe, userAgent);
  }

  /**
   * Déconnecte tous ses autres appareils (téléphone perdu, ordinateur prêté...) : leurs sessions et
   * leurs tokens d'accès ne marchent plus. Cet appareil-ci reçoit une nouvelle session.
   */
  public Connexion deconnecterAutresAppareils(String userAgent) {
    Employe employe = currentEmploye.get();
    employe.setCredentialsChangedAt(OffsetDateTime.now(clock));
    sessionService.fermerToutes(employe);
    employeRepository.flush();
    log.info("Employé {} : tous ses autres appareils ont été déconnectés", employe.getId());
    return connexion(employe, userAgent);
  }

  private boolean verifier(String motDePasse, String hash) {
    try {
      return passwordEncoder.matches(motDePasse, hash);
    } catch (IllegalArgumentException e) {
      return false; // mot de passe trop long pour bcrypt, hash illisible...
    }
  }

  private Connexion connexion(Employe employe, String userAgent) {
    return new Connexion(reponse(employe), sessionService.ouvrir(employe, userAgent));
  }

  private LoginResponse reponse(Employe employe) {
    JwtService.TokenEmis token = jwtService.emettre(employe);
    return new LoginResponse(token.token(), token.expireLe(), EmployeService.toDto(employe));
  }
}

package pcpro.atelier.features.auth.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.SecurityProperties;
import pcpro.atelier.features.auth.domain.SessionEmploye;
import pcpro.atelier.features.auth.infra.SessionEmployeRepository;
import pcpro.atelier.features.employe.domain.Employe;

/**
 * Sessions longues ("rester connecté") à base de refresh tokens à usage unique.
 *
 * Chaque refresh renvoie un nouveau token et invalide l'ancien (rotation). Si un token déjà utilisé
 * est présenté à nouveau, c'est qu'il a été copié : toutes les sessions de l'employé sont révoquées.
 */
@Service
@Transactional
public class SessionService {

  private static final Logger log = LoggerFactory.getLogger(SessionService.class);

  /** Deux onglets qui rafraîchissent en même temps ne doivent pas être pris pour un vol de token. */
  private static final Duration DELAI_GRACE_ROTATION = Duration.ofSeconds(30);

  public record SessionOuverte(Employe employe, String refreshToken) {
  }

  private final SessionEmployeRepository sessionRepository;
  private final SecurityProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public SessionService(SessionEmployeRepository sessionRepository, SecurityProperties properties, Clock clock) {
    this.sessionRepository = sessionRepository;
    this.properties = properties;
    this.clock = clock;
  }

  public String ouvrir(Employe employe, String userAgent) {
    byte[] octets = new byte[32];
    random.nextBytes(octets);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

    SessionEmploye session = new SessionEmploye();
    session.setEmploye(employe);
    session.setTokenHash(empreinte(token));
    session.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
    session.setExpiresAt(OffsetDateTime.now(clock).plus(properties.refreshTtl()));
    sessionRepository.save(session);
    return token;
  }

  /**
   * Échange un refresh token valide contre un nouveau (l'ancien est révoqué).
   *
   * @throws BadCredentialsException si le token est inconnu, expiré, révoqué ou l'employé désactivé
   */
  @Transactional(noRollbackFor = BadCredentialsException.class)
  public SessionOuverte renouveler(String refreshToken, String userAgent) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BadCredentialsException("Session expirée, reconnectez-vous");
    }
    SessionEmploye session = sessionRepository.findByTokenHash(empreinte(refreshToken))
        .orElseThrow(() -> new BadCredentialsException("Session expirée, reconnectez-vous"));
    Employe employe = session.getEmploye();
    OffsetDateTime maintenant = OffsetDateTime.now(clock);

    if (session.getRevokedAt() != null) {
      // Seul un token déjà échangé trahit une copie ; un token fermé (logout, mot de passe changé) est juste périmé
      if (session.isRemplacee() && session.getRevokedAt().plus(DELAI_GRACE_ROTATION).isBefore(maintenant)) {
        log.warn("Réutilisation d'un refresh token révoqué pour l'employé {} : toutes ses sessions sont fermées",
            employe.getId());
        sessionRepository.revoquerToutes(employe, maintenant);
      }
      throw new BadCredentialsException("Session expirée, reconnectez-vous");
    }
    if (session.getExpiresAt().isBefore(maintenant) || !employe.isActif()) {
      throw new BadCredentialsException("Session expirée, reconnectez-vous");
    }

    session.setRevokedAt(maintenant);
    session.setRemplacee(true);
    return new SessionOuverte(employe, ouvrir(employe, userAgent));
  }

  /** Déconnexion de l'appareil courant. Ne fait rien si le token est inconnu. */
  public void fermer(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    sessionRepository.findByTokenHash(empreinte(refreshToken))
        .filter(s -> s.getRevokedAt() == null)
        .ifPresent(s -> s.setRevokedAt(OffsetDateTime.now(clock)));
  }

  /** Déconnecte l'employé de tous ses appareils (mot de passe changé, compte désactivé). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void fermerToutes(Employe employe) {
    sessionRepository.revoquerToutes(employe, OffsetDateTime.now(clock));
  }

  /** Ménage quotidien des sessions expirées ou révoquées depuis plus d'une semaine. */
  @Scheduled(cron = "0 30 3 * * *")
  public void purger() {
    int supprimees = sessionRepository.supprimerAnciennes(OffsetDateTime.now(clock).minusDays(7));
    if (supprimees > 0) {
      log.info("{} sessions expirées supprimées", supprimees);
    }
  }

  static String empreinte(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}

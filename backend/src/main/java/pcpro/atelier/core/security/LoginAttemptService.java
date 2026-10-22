package pcpro.atelier.core.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import pcpro.atelier.core.config.SecurityProperties;

/**
 * Anti brute-force : après N échecs sur un même login, ou trop d'échecs depuis une même adresse IP
 * (quelqu'un qui essaie plein de logins), la connexion est bloquée quelques minutes.
 * En mémoire, ce qui suffit pour une instance unique de l'application.
 */
@Component
public class LoginAttemptService {

  private record Tentatives(int echecs, Instant bloqueJusqua) {
  }

  private static final int MAX_CLES_SUIVIES = 10_000;

  private final ConcurrentHashMap<String, Tentatives> tentatives = new ConcurrentHashMap<>();
  private final SecurityProperties properties;
  private final Clock clock;

  public LoginAttemptService(SecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public boolean estBloque(String login, String ip) {
    return bloque(cleLogin(login)) || bloque(cleIp(ip));
  }

  public void echec(String login, String ip) {
    if (tentatives.size() > MAX_CLES_SUIVIES) {
      purgerExpires();
    }
    enregistrerEchec(cleLogin(login), properties.loginMaxEchecs());
    enregistrerEchec(cleIp(ip), properties.loginMaxEchecsParIp());
  }

  public void succes(String login) {
    tentatives.remove(cleLogin(login));
  }

  /** Oublie tous les échecs et blocages (tests, ou après un redémarrage manuel). */
  public void reinitialiser() {
    tentatives.clear();
  }

  private boolean bloque(String cle) {
    Tentatives t = tentatives.get(cle);
    return t != null && t.bloqueJusqua() != null && t.bloqueJusqua().isAfter(clock.instant());
  }

  private void enregistrerEchec(String cle, int maxEchecs) {
    tentatives.compute(cle, (k, t) -> {
      Instant now = clock.instant();
      // Blocage expiré : on repart de zéro
      int echecs = (t == null || (t.bloqueJusqua() != null && !t.bloqueJusqua().isAfter(now))) ? 1 : t.echecs() + 1;
      Instant bloqueJusqua = echecs >= maxEchecs ? now.plus(properties.loginBlocage()) : null;
      return new Tentatives(echecs, bloqueJusqua);
    });
  }

  /** Évite que la map grossisse sans fin si quelqu'un essaie des milliers de logins différents. */
  private void purgerExpires() {
    Instant now = clock.instant();
    tentatives.entrySet().removeIf(e -> e.getValue().bloqueJusqua() == null || !e.getValue().bloqueJusqua().isAfter(now));
  }

  private static String cleLogin(String login) {
    return "login:" + (login == null ? "" : login.trim().toLowerCase(Locale.ROOT));
  }

  private static String cleIp(String ip) {
    return "ip:" + (ip == null ? "" : ip);
  }
}

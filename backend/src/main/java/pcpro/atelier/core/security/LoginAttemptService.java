package pcpro.atelier.core.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import pcpro.atelier.core.config.SecurityProperties;

/**
 * Anti brute-force simple : après N échecs sur un même login, celui-ci est bloqué quelques minutes.
 * En mémoire, ce qui suffit pour une instance unique de l'application.
 */
@Component
public class LoginAttemptService {

  private record Tentatives(int echecs, Instant bloqueJusqua) {
  }

  private static final int MAX_LOGINS_SUIVIS = 10_000;

  private final ConcurrentHashMap<String, Tentatives> tentatives = new ConcurrentHashMap<>();
  private final SecurityProperties properties;
  private final Clock clock;

  public LoginAttemptService(SecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public boolean estBloque(String login) {
    Tentatives t = tentatives.get(cle(login));
    return t != null && t.bloqueJusqua() != null && t.bloqueJusqua().isAfter(clock.instant());
  }

  public void echec(String login) {
    if (tentatives.size() > MAX_LOGINS_SUIVIS) {
      purgerExpires();
    }
    tentatives.compute(cle(login), (k, t) -> {
      Instant now = clock.instant();
      // Blocage expiré : on repart de zéro
      int echecs = (t == null || (t.bloqueJusqua() != null && !t.bloqueJusqua().isAfter(now))) ? 1 : t.echecs() + 1;
      Instant bloqueJusqua = echecs >= properties.loginMaxEchecs() ? now.plus(properties.loginBlocage()) : null;
      return new Tentatives(echecs, bloqueJusqua);
    });
  }

  public void succes(String login) {
    tentatives.remove(cle(login));
  }

  /** Évite que la map grossisse sans fin si quelqu'un essaie des milliers de logins différents. */
  private void purgerExpires() {
    Instant now = clock.instant();
    tentatives.entrySet().removeIf(e -> e.getValue().bloqueJusqua() == null || !e.getValue().bloqueJusqua().isAfter(now));
  }

  private static String cle(String login) {
    return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
  }
}

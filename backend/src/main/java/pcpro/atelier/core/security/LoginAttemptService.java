package pcpro.atelier.core.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pcpro.atelier.core.config.SecurityProperties;

/**
 * Anti brute-force, avec trois compteurs d'échecs :
 * - un login depuis une adresse IP (le cas normal : quelqu'un qui essaie des mots de passe) ;
 * - une adresse IP, tous logins confondus (quelqu'un qui essaie plein de logins) ;
 * - un login, toutes adresses confondues, avec un seuil plus haut (attaque répartie sur beaucoup d'adresses).
 *
 * Ainsi un inconnu qui se trompe exprès ne bloque pas le compte de l'employé, qui se connecte depuis une
 * autre adresse. Il faut pour cela que l'application voie la vraie adresse du client derrière le
 * reverse proxy (voir infra/Caddyfile) : sinon tout le monde a la même adresse et un seul attaquant
 * bloque tout le monde.
 *
 * En mémoire, ce qui suffit pour une instance unique de l'application.
 */
@Component
public class LoginAttemptService {

  private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

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
    return bloque(cleLoginIp(login, ip)) || bloque(cleIp(ip)) || bloque(cleLogin(login));
  }

  public void echec(String login, String ip) {
    if (tentatives.size() > MAX_CLES_SUIVIES) {
      purgerExpires();
    }
    enregistrerEchec(cleLoginIp(login, ip), properties.loginMaxEchecs());
    enregistrerEchec(cleIp(ip), properties.loginMaxEchecsParIp());
    enregistrerEchec(cleLogin(login), properties.loginMaxEchecsParCompte());
  }

  public void succes(String login, String ip) {
    tentatives.remove(cleLoginIp(login, ip));
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
    Tentatives resultat = tentatives.compute(cle, (k, t) -> {
      Instant now = clock.instant();
      // Blocage expiré : on repart de zéro
      int echecs = (t == null || (t.bloqueJusqua() != null && !t.bloqueJusqua().isAfter(now))) ? 1 : t.echecs() + 1;
      Instant bloqueJusqua = echecs >= maxEchecs ? now.plus(properties.loginBlocage()) : null;
      return new Tentatives(echecs, bloqueJusqua);
    });
    if (resultat.echecs() == maxEchecs) {
      log.warn("Trop d'échecs pour {} : bloqué pendant {}", nettoyer(cle), properties.loginBlocage());
    }
  }

  /** Évite que la map grossisse sans fin si quelqu'un essaie des milliers de logins différents. */
  private void purgerExpires() {
    Instant now = clock.instant();
    tentatives.entrySet().removeIf(e -> e.getValue().bloqueJusqua() == null || !e.getValue().bloqueJusqua().isAfter(now));
  }

  /** Texte saisi par un inconnu mis dans les logs : sans retours à la ligne ni caractères de contrôle. */
  public static String nettoyer(String texte) {
    if (texte == null) {
      return "";
    }
    String propre = texte.replaceAll("\\p{Cntrl}", "?");
    return propre.length() > 80 ? propre.substring(0, 80) + "…" : propre;
  }

  private static String normaliser(String login) {
    return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
  }

  private static String cleLoginIp(String login, String ip) {
    return "login:" + normaliser(login) + "@" + (ip == null ? "" : ip);
  }

  private static String cleLogin(String login) {
    return "login:" + normaliser(login);
  }

  private static String cleIp(String ip) {
    return "ip:" + (ip == null ? "" : ip);
  }
}

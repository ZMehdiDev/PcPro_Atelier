package pcpro.atelier.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;
import pcpro.atelier.core.enums.Role;
import pcpro.atelier.features.auth.api.AuthController;
import pcpro.atelier.support.IntegrationTest;

/**
 * "Rester connecté" : refresh token en cookie HttpOnly, rotation, détection de vol, déconnexion.
 */
class SessionIntegrationTest extends IntegrationTest {

  private MockHttpServletResponse connexion(String login, String motDePasse) throws Exception {
    return mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", login, "motDePasse", motDePasse))))
        .andExpect(status().isOk())
        .andReturn().getResponse();
  }

  private static Cookie cookie(MockHttpServletResponse response) {
    Cookie cookie = response.getCookie(AuthController.REFRESH_COOKIE);
    assertThat(cookie).isNotNull();
    return cookie;
  }

  private static MockHttpServletRequestBuilder refresh(Cookie cookie) {
    return post("/api/auth/refresh").cookie(cookie);
  }

  @Test
  void loginPoseUnCookieDeSessionProtege() throws Exception {
    String setCookie = connexion("tech", TECH_PASSWORD).getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .startsWith(AuthController.REFRESH_COOKIE + "=")
        .contains("Path=/api/auth", "HttpOnly", "Secure", "SameSite=Strict", "Max-Age=2592000");
  }

  @Test
  void refreshDonneUnNouveauTokenEtUnNouveauCookie() throws Exception {
    Cookie premier = cookie(connexion("tech", TECH_PASSWORD));

    MockHttpServletResponse reponse = mvc.perform(refresh(premier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employe.login").value("tech"))
        .andReturn().getResponse();
    Cookie second = cookie(reponse);
    assertThat(second.getValue()).isNotEqualTo(premier.getValue());

    String token = read(reponse.getContentAsString(), "$.token");
    mvc.perform(as(token, get("/api/auth/moi"))).andExpect(status().isOk());
    mvc.perform(refresh(second)).andExpect(status().isOk());
  }

  @Test
  void refreshSansCookieOuAvecCookieInconnuRefuse() throws Exception {
    mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    mvc.perform(refresh(new Cookie(AuthController.REFRESH_COOKIE, "inconnu")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Session expirée, reconnectez-vous"));
  }

  @Test
  void ancienCookieRejoueJusteApresLaRotationEstRefuseSansToutCasser() throws Exception {
    Cookie premier = cookie(connexion("tech", TECH_PASSWORD));
    Cookie second = cookie(mvc.perform(refresh(premier)).andReturn().getResponse());

    // Deux onglets qui rafraîchissent en même temps : l'un perd, l'autre garde sa session
    mvc.perform(refresh(premier)).andExpect(status().isUnauthorized());
    mvc.perform(refresh(second)).andExpect(status().isOk());
  }

  @Test
  void ancienCookieRejoueBienPlusTardFermeToutesLesSessions() throws Exception {
    Cookie vole = cookie(connexion("tech", TECH_PASSWORD));
    Cookie legitime = cookie(mvc.perform(refresh(vole)).andReturn().getResponse());
    Cookie autreAppareil = cookie(connexion("tech", TECH_PASSWORD));
    jdbcClient.sql("UPDATE session_employe SET revoked_at = revoked_at - INTERVAL '5 minutes' WHERE remplacee")
        .update();

    mvc.perform(refresh(vole)).andExpect(status().isUnauthorized());
    mvc.perform(refresh(legitime)).andExpect(status().isUnauthorized());
    mvc.perform(refresh(autreAppareil)).andExpect(status().isUnauthorized());
    connexion("tech", TECH_PASSWORD);
  }

  @Test
  void logoutFermeLaSessionEtEffaceLeCookie() throws Exception {
    Cookie session = cookie(connexion("tech", TECH_PASSWORD));

    MockHttpServletResponse reponse = mvc.perform(post("/api/auth/logout").cookie(session))
        .andExpect(status().isNoContent())
        .andReturn().getResponse();
    assertThat(cookie(reponse).getMaxAge()).isZero();

    mvc.perform(refresh(session)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
  }

  @Test
  void changerDeMotDePasseDeconnecteLesAutresAppareils() throws Exception {
    Cookie telephone = cookie(connexion("tech", TECH_PASSWORD));

    MockHttpServletResponse reponse = mvc.perform(json(techToken, post("/api/auth/mot-de-passe"),
            Map.of("ancienMotDePasse", TECH_PASSWORD, "nouveauMotDePasse", "nouveau-mdp-123")))
        .andExpect(status().isOk())
        .andReturn().getResponse();
    Cookie pc = cookie(reponse);

    mvc.perform(refresh(telephone)).andExpect(status().isUnauthorized());
    // Le vieux cookie du téléphone ne doit pas être pris pour un vol et couper le PC
    jdbcClient.sql("UPDATE session_employe SET revoked_at = revoked_at - INTERVAL '5 minutes'").update();
    mvc.perform(refresh(telephone)).andExpect(status().isUnauthorized());
    mvc.perform(refresh(pc)).andExpect(status().isOk());
  }

  @Test
  void employeDesactiveNePeutPlusRafraichir() throws Exception {
    Cookie session = cookie(connexion("tech", TECH_PASSWORD));
    mvc.perform(json(adminToken, put("/api/employes/" + tech.getId()),
            Map.of("prenom", "Frere", "role", "TECHNICIEN", "actif", false)))
        .andExpect(status().isOk());

    mvc.perform(refresh(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void tropDEchecsDepuisUneMemeAdresseIpBloquentCetteAdresse() throws Exception {
    creerEmploye("Pere", "pere", "bon-mot-de-passe", Role.TECHNICIEN);
    for (int i = 0; i < 5; i++) {
      mvc.perform(post("/api/auth/login")
              .with(r -> { r.setRemoteAddr("203.0.113.7"); return r; })
              .contentType(MediaType.APPLICATION_JSON)
              .content(json(Map.of("login", "essai" + i, "motDePasse", "faux"))))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(post("/api/auth/login")
            .with(r -> { r.setRemoteAddr("203.0.113.7"); return r; })
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", "pere", "motDePasse", "bon-mot-de-passe"))))
        .andExpect(status().isTooManyRequests());
    // Depuis l'atelier (autre adresse), pas de blocage
    connexion("pere", "bon-mot-de-passe");
  }
}

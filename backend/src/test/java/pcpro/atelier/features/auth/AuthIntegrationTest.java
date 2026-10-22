package pcpro.atelier.features.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import pcpro.atelier.core.enums.Role;
import pcpro.atelier.support.IntegrationTest;

class AuthIntegrationTest extends IntegrationTest {

  @Test
  void loginRenvoieUnTokenEtLEmploye() throws Exception {
    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", " ADMIN ", "motDePasse", ADMIN_PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.expireLe").isNotEmpty())
        .andExpect(jsonPath("$.employe.login").value("admin"))
        .andExpect(jsonPath("$.employe.role").value("ADMIN"))
        .andExpect(jsonPath("$.employe.passwordHash").doesNotExist());
  }

  @Test
  void mauvaisMotDePasseEtLoginInconnuDonnentLaMemeErreur() throws Exception {
    for (String login : new String[] {"admin", "inconnu"}) {
      mvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(json(Map.of("login", login, "motDePasse", "mauvais-mdp"))))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Login ou mot de passe incorrect"));
    }
  }

  @Test
  void loginBloqueApresTropDEchecs() throws Exception {
    creerEmploye("Victime", "victime", "bon-mot-de-passe", Role.TECHNICIEN);
    for (int i = 0; i < 3; i++) {
      mvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(json(Map.of("login", "victime", "motDePasse", "faux-" + i))))
          .andExpect(status().isUnauthorized());
    }
    // Même le bon mot de passe est refusé pendant le blocage
    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", "victime", "motDePasse", "bon-mot-de-passe"))))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void sansTokenLApiRepond401EnJson() throws Exception {
    mvc.perform(get("/api/dossiers"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void tokenFalsifieRefuse() throws Exception {
    mvc.perform(as(adminToken + "x", get("/api/dossiers")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void moiRenvoieLEmployeConnecte() throws Exception {
    mvc.perform(as(techToken, get("/api/auth/moi")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.login").value("tech"))
        .andExpect(jsonPath("$.role").value("TECHNICIEN"));
  }

  @Test
  void changerSonMotDePasseRevoqueLesAnciensTokens() throws Exception {
    Thread.sleep(1100); // les dates des tokens JWT sont à la seconde

    String body = mvc.perform(json(techToken, post("/api/auth/mot-de-passe"),
            Map.of("ancienMotDePasse", TECH_PASSWORD, "nouveauMotDePasse", "nouveau-mdp-123")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    String nouveauToken = read(body, "$.token");

    mvc.perform(as(techToken, get("/api/auth/moi"))).andExpect(status().isUnauthorized());
    mvc.perform(as(nouveauToken, get("/api/auth/moi"))).andExpect(status().isOk());
    login("tech", "nouveau-mdp-123");
  }

  @Test
  void changerMotDePasseAvecMauvaisAncienRefuse() throws Exception {
    mvc.perform(json(techToken, post("/api/auth/mot-de-passe"),
            Map.of("ancienMotDePasse", "faux", "nouveauMotDePasse", "nouveau-mdp-123")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void employeDesactiveNePeutPlusUtiliserSonToken() throws Exception {
    mvc.perform(json(adminToken, put("/api/employes/" + tech.getId()),
            Map.of("prenom", "Frere", "role", "TECHNICIEN", "actif", false)))
        .andExpect(status().isOk());

    mvc.perform(as(techToken, get("/api/dossiers"))).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", "tech", "motDePasse", TECH_PASSWORD))))
        .andExpect(status().isUnauthorized());
  }
}

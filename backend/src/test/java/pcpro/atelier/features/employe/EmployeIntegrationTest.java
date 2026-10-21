package pcpro.atelier.features.employe;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import pcpro.atelier.support.IntegrationTest;

class EmployeIntegrationTest extends IntegrationTest {

  private static final Map<String, Object> NOUVEAU = Map.of(
      "prenom", "Pere", "login", "Pere", "motDePasse", "mot-de-passe-pere", "role", "TECHNICIEN");

  @Test
  void seulUnAdminPeutCreerUnEmploye() throws Exception {
    mvc.perform(json(techToken, post("/api/employes"), NOUVEAU))
        .andExpect(status().isForbidden());

    mvc.perform(json(adminToken, post("/api/employes"), NOUVEAU))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.login").value("pere"))
        .andExpect(jsonPath("$.motDePasseDefini").value(true));
    login("pere", "mot-de-passe-pere");

    mvc.perform(json(adminToken, post("/api/employes"), NOUVEAU))
        .andExpect(status().isConflict());
  }

  @Test
  void motDePasseTropCourtRefuse() throws Exception {
    mvc.perform(json(adminToken, post("/api/employes"),
            Map.of("prenom", "Pere", "login", "pere", "motDePasse", "court", "role", "TECHNICIEN")))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void listeDesActifsPourTousLesInactifsPourLesAdmins() throws Exception {
    mvc.perform(json(adminToken, put("/api/employes/" + tech.getId()),
        Map.of("prenom", "Frere", "role", "TECHNICIEN", "actif", false))).andExpect(status().isOk());

    mvc.perform(as(adminToken, get("/api/employes"))).andExpect(jsonPath("$", hasSize(1)));
    mvc.perform(as(adminToken, get("/api/employes").param("inclureInactifs", "true"))).andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void unTechnicienNeVoitPasLesInactifs() throws Exception {
    mvc.perform(as(techToken, get("/api/employes").param("inclureInactifs", "true")))
        .andExpect(status().isForbidden());
  }

  @Test
  void unAdminNePeutPasSeRetirerSesDroits() throws Exception {
    mvc.perform(json(adminToken, put("/api/employes/" + admin.getId()),
            Map.of("prenom", "Zak", "role", "TECHNICIEN", "actif", true)))
        .andExpect(status().isConflict());
    mvc.perform(json(adminToken, put("/api/employes/" + admin.getId()),
            Map.of("prenom", "Zak", "role", "ADMIN", "actif", false)))
        .andExpect(status().isConflict());
  }

  @Test
  void reinitialiserLeMotDePasseDUnEmploye() throws Exception {
    mvc.perform(json(adminToken, post("/api/employes/" + tech.getId() + "/mot-de-passe"),
            Map.of("motDePasse", "nouveau-mdp-tech")))
        .andExpect(status().isNoContent());
    login("tech", "nouveau-mdp-tech");
  }

  @Test
  void unTechnicienPromuAdminObtientLesDroitsImmediatement() throws Exception {
    mvc.perform(json(adminToken, put("/api/employes/" + tech.getId()),
        Map.of("prenom", "Frere", "role", "ADMIN", "actif", true))).andExpect(status().isOk());
    // Le rôle est relu en base : pas besoin de se reconnecter
    mvc.perform(json(techToken, post("/api/employes"), NOUVEAU)).andExpect(status().isCreated());
  }
}

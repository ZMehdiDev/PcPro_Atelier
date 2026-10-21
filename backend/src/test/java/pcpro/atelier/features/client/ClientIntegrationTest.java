package pcpro.atelier.features.client;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class ClientIntegrationTest extends IntegrationTest {

  @BeforeEach
  void clients() throws Exception {
    mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Martin Dubois", "0470 12 34 56")));
    mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Sophie Laurent", "+32 480 99 98 88")));
  }

  @Test
  void rechercheParNomOuTelephone() throws Exception {
    mvc.perform(as(techToken, get("/api/clients"))).andExpect(jsonPath("$.content", hasSize(2)));
    mvc.perform(as(techToken, get("/api/clients").param("q", "dubois")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].telephone").value("0470123456"));
    mvc.perform(as(techToken, get("/api/clients").param("q", "480 99")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].telephone").value("+32480999888"));
  }

  @Test
  void modifierUnClient() throws Exception {
    mvc.perform(json(techToken, put("/api/clients/1"),
            Map.of("nom", "Martin Dubois", "telephone", "0470 00 00 00", "email", "martin@example.be")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.telephone").value("0470000000"))
        .andExpect(jsonPath("$.email").value("martin@example.be"));
  }

  @Test
  void telephoneDejaUtiliseOuEmailInvalideRefuses() throws Exception {
    mvc.perform(json(techToken, put("/api/clients/1"),
            Map.of("nom", "Martin Dubois", "telephone", "+32 480 999 888")))
        .andExpect(status().isConflict());
    mvc.perform(json(techToken, put("/api/clients/1"),
            Map.of("nom", "Martin Dubois", "telephone", "0470123456", "email", "pas-un-email")))
        .andExpect(status().isUnprocessableContent());
  }
}

package pcpro.atelier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import pcpro.atelier.support.IntegrationTest;

class AtelierApplicationTests extends IntegrationTest {

  @Test
  void demarreAvecLesMigrationsFlyway() throws Exception {
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OK"));
  }

  @Test
  void swaggerEstDesactiveParDefaut() throws Exception {
    mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }
}

package pcpro.atelier.features.bondepot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class BonDepotIntegrationTest extends IntegrationTest {

  @Test
  void bonDeDepotEnPdf() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossierAvecEmail("Martin Dübois", "0470123456", "martin@example.be"));

    byte[] pdf = mvc.perform(as(techToken, get("/api/dossiers/" + id + "/bon-depot")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline; filename=\"bon-depot-PCP-")))
        .andReturn().getResponse().getContentAsByteArray();

    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    assertThat(pdf.length).isGreaterThan(2000);
  }

  @Test
  void bonDeDepotProtege() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(get("/api/dossiers/" + id + "/bon-depot")).andExpect(status().isUnauthorized());
    mvc.perform(as(techToken, get("/api/dossiers/999/bon-depot"))).andExpect(status().isNotFound());
  }
}

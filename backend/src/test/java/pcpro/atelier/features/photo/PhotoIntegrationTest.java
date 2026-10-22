package pcpro.atelier.features.photo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class PhotoIntegrationTest extends IntegrationTest {

  /** Plus petit PNG valide (1x1 pixel). */
  private static final byte[] PNG = {
      (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R',
      0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
  };

  private long dossierId;

  @BeforeEach
  void dossier() throws Exception {
    String body = mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Martin", "0470123456")))
        .andReturn().getResponse().getContentAsString();
    dossierId = ((Number) read(body, "$.id")).longValue();
  }

  @Test
  void uploadTelechargementEtSuppression() throws Exception {
    String body = mvc.perform(as(techToken, multipart("/api/dossiers/" + dossierId + "/photos")
            .file(new MockMultipartFile("file", "ecran.png", "image/png", PNG))
            .param("description", "Écran fissuré")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mimeType").value("image/png"))
        .andExpect(jsonPath("$.tailleBytes").value(PNG.length))
        .andExpect(jsonPath("$.cheminFichier").doesNotExist())
        .andReturn().getResponse().getContentAsString();
    String url = read(body, "$.url");

    mvc.perform(as(techToken, get(url)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(content().bytes(PNG));
    mvc.perform(get(url)).andExpect(status().isUnauthorized());

    long photoId = ((Number) read(body, "$.id")).longValue();
    mvc.perform(as(techToken, delete("/api/dossiers/" + dossierId + "/photos/" + photoId)))
        .andExpect(status().isNoContent());
    mvc.perform(as(techToken, get(url))).andExpect(status().isNotFound());
    mvc.perform(as(techToken, get("/api/dossiers/" + dossierId)))
        .andExpect(jsonPath("$.photos").isEmpty())
        .andExpect(jsonPath("$.evenements[0].typeAction").value("SUPPRESSION_PHOTO"));
  }

  @Test
  void faussImageRefuseeMalgreLeContentType() throws Exception {
    byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
    mvc.perform(as(techToken, multipart("/api/dossiers/" + dossierId + "/photos")
            .file(new MockMultipartFile("file", "photo.png", "image/png", script))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void fichierVideRefuse() throws Exception {
    mvc.perform(as(techToken, multipart("/api/dossiers/" + dossierId + "/photos")
            .file(new MockMultipartFile("file", "vide.jpg", "image/jpeg", new byte[0]))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void photoDUnAutreDossierIntrouvable() throws Exception {
    String body = mvc.perform(as(techToken, multipart("/api/dossiers/" + dossierId + "/photos")
            .file(new MockMultipartFile("file", "a.png", "image/png", PNG))))
        .andReturn().getResponse().getContentAsString();
    long photoId = ((Number) read(body, "$.id")).longValue();
    mvc.perform(as(techToken, get("/api/dossiers/" + (dossierId + 1) + "/photos/" + photoId + "/contenu")))
        .andExpect(status().isNotFound());
  }
}

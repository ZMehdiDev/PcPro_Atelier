package pcpro.atelier.features.suivi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

/**
 * Suivi en ligne par le client, sans compte.
 */
class SuiviIntegrationTest extends IntegrationTest {

  private long id;
  private String reference;
  private String code;

  @BeforeEach
  void creerDossierAvecDevis() throws Exception {
    id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470 12 34 56"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"),
        Map.of("lignes", List.of(Fixtures.ligneDevis("MAIN_OEUVRE", "Remplacement clavier", "75"))))).andExpect(status().isOk());
    String detail = mvc.perform(as(techToken, get("/api/dossiers/" + id))).andReturn().getResponse().getContentAsString();
    reference = read(detail, "$.reference");
    code = read(detail, "$.codeSuivi");
    org.assertj.core.api.Assertions.assertThat((String) read(detail, "$.lienSuivi"))
        .isEqualTo("https://atelier.test/suivi/" + reference + "?code=" + code);
  }

  @Test
  void suiviAvecLeCodeSansEtreConnecte() throws Exception {
    mvc.perform(get("/api/public/suivi/" + reference).param("code", code.toLowerCase()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reference").value(reference))
        .andExpect(jsonPath("$.appareil").value("ordinateur portable Lenovo ThinkPad T14"))
        .andExpect(jsonPath("$.statut").value("ATTENTE_CLIENT"))
        .andExpect(jsonPath("$.statutLibelle").value("En attente de votre réponse"))
        .andExpect(jsonPath("$.devis.total").value(75))
        .andExpect(jsonPath("$.peutRepondreDevis").value(true))
        .andExpect(jsonPath("$.atelier.telephone").value("02 123 45 67"))
        // Rien d'interne : ni notes, ni historique, ni client
        .andExpect(jsonPath("$.notes").doesNotExist())
        .andExpect(jsonPath("$.evenements").doesNotExist())
        .andExpect(jsonPath("$.client").doesNotExist());
  }

  @Test
  void suiviAvecLeTelephoneSousToutesSesFormes() throws Exception {
    for (String telephone : new String[] {"0470123456", "+32 470 12 34 56", "0032470/12.34.56"}) {
      mvc.perform(get("/api/public/suivi/" + reference.toLowerCase()).param("telephone", telephone))
          .andExpect(status().isOk())
          // Le téléphone peut être connu d'autres personnes : pas de réponse au devis par ce biais
          .andExpect(jsonPath("$.peutRepondreDevis").value(false));
    }
  }

  @Test
  void mauvaisCodeOuReferenceDonnentLaMemeErreurPuisBloquent() throws Exception {
    mvc.perform(get("/api/public/suivi/" + reference).param("code", "MAUVAIS1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Aucun dossier ne correspond, vérifiez la référence et le code"));
    mvc.perform(get("/api/public/suivi/PCP-0000-999999").param("code", code)).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/suivi/" + reference).param("telephone", "0499999999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/suivi/" + reference)).andExpect(status().isNotFound());

    // 3 essais ratés sur ce dossier (configuration de test) : bloqué, même avec le bon code
    mvc.perform(get("/api/public/suivi/" + reference).param("code", code)).andExpect(status().isTooManyRequests());
  }

  @Test
  void leClientAccepteLeDevisEnLigne() throws Exception {
    mvc.perform(post("/api/public/suivi/" + reference + "/devis")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("code", code, "accepte", true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.devis.statut").value("ACCEPTE"))
        .andExpect(jsonPath("$.statut").value("REPARATION"))
        .andExpect(jsonPath("$.peutRepondreDevis").value(false))
        .andExpect(jsonPath("$.resteAPayer").value(75));

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.evenements[0].typeAction").value("CHANGEMENT_STATUT"))
        .andExpect(jsonPath("$.evenements[0].employePrenom").value("Client"))
        .andExpect(jsonPath("$.evenements[1].typeAction").value("DECISION_DEVIS"))
        .andExpect(jsonPath("$.evenements[1].employePrenom").value("Client"));

    // Une seule réponse possible
    mvc.perform(post("/api/public/suivi/" + reference + "/devis")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("code", code, "accepte", false))))
        .andExpect(status().isConflict());
  }

  @Test
  void reponseAuDevisRefuseeSansLeBonCode() throws Exception {
    mvc.perform(post("/api/public/suivi/" + reference + "/devis")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("code", "0470123456", "accepte", true))))
        .andExpect(status().isNotFound());
    mvc.perform(as(techToken, get("/api/dossiers/" + id))).andExpect(jsonPath("$.devis.statut").value("EN_ATTENTE"));
  }
}

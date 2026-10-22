package pcpro.atelier.features.dossier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class DossierIntegrationTest extends IntegrationTest {

  private long creerDossier(String nom, String telephone) throws Exception {
    String body = mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier(nom, telephone)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) read(body, "$.id")).longValue();
  }

  private void changerStatut(String token, long id, String statut, int attendu) throws Exception {
    mvc.perform(json(token, patch("/api/dossiers/" + id + "/statut"), Map.of("statut", statut)))
        .andExpect(status().is(attendu));
  }

  @Test
  void creationDossierAvecReferenceDatesEtHistorique() throws Exception {
    mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Martin Dubois", "0470 12 34 56")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reference").value(matchesPattern("PCP-\\d{4}-000001")))
        .andExpect(jsonPath("$.statut").value("RECU"))
        .andExpect(jsonPath("$.priorite").value("NORMALE"))
        .andExpect(jsonPath("$.clientTelephone").value("0470123456"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    mvc.perform(as(techToken, get("/api/dossiers/1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.descriptionProbleme").value("Ne démarre plus"))
        .andExpect(jsonPath("$.accessoires").value("Chargeur"))
        .andExpect(jsonPath("$.statutsPossibles", containsInAnyOrder("DIAGNOSTIC", "ATTENTE_CLIENT", "PRET")))
        .andExpect(jsonPath("$.evenements", hasSize(1)))
        .andExpect(jsonPath("$.evenements[0].typeAction").value("CREATION_DOSSIER"))
        .andExpect(jsonPath("$.evenements[0].employePrenom").value("Frere"));
  }

  @Test
  void referencesSequentiellesEtClientRetrouveParTelephone() throws Exception {
    creerDossier("Martin Dubois", "0470123456");
    String body = mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Martin Dubois", "0470/12.34.56")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reference").value(matchesPattern("PCP-\\d{4}-000002")))
        .andReturn().getResponse().getContentAsString();

    Number clientId = read(body, "$.clientId");
    mvc.perform(as(techToken, get("/api/clients/" + clientId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dossiers", hasSize(2)));
  }

  @Test
  void creationAvecClientExistant() throws Exception {
    creerDossier("Martin Dubois", "0470123456");
    Map<String, Object> body = new HashMap<>(Fixtures.nouveauDossier("x", "x"));
    body.remove("client");
    body.put("clientId", 1);
    mvc.perform(json(techToken, post("/api/dossiers"), body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clientNom").value("Martin Dubois"));
  }

  @Test
  void creationSansClientOuAvecLesDeuxRefusee() throws Exception {
    Map<String, Object> body = new HashMap<>(Fixtures.nouveauDossier("x", "0470123456"));
    body.put("clientId", 1);
    mvc.perform(json(techToken, post("/api/dossiers"), body)).andExpect(status().isBadRequest());
    body.remove("clientId");
    body.remove("client");
    mvc.perform(json(techToken, post("/api/dossiers"), body)).andExpect(status().isBadRequest());
  }

  @Test
  void validationDesChampsEn422() throws Exception {
    Map<String, Object> body = new HashMap<>(Fixtures.nouveauDossier("", "0470123456"));
    body.put("appareil", Map.of("marque", "Dell"));
    mvc.perform(json(techToken, post("/api/dossiers"), body))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("client.nom", "appareil.type")));
  }

  @Test
  void workflowCompletJusquALaCloture() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    for (String statut : new String[] {"DIAGNOSTIC", "REPARATION", "TESTS", "PRET", "CLOTURE"}) {
      changerStatut(techToken, id, statut, 200);
    }
    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.statut").value("CLOTURE"))
        .andExpect(jsonPath("$.closedAt").isNotEmpty())
        .andExpect(jsonPath("$.evenements[0].typeAction").value("CLOTURE"));
  }

  @Test
  void transitionInterditeRefusee() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    changerStatut(techToken, id, "TESTS", 409);
    changerStatut(techToken, id, "CLOTURE", 409);
    changerStatut(techToken, id, "RECU", 409);
  }

  @Test
  void dossierClotureEnLectureSeuleEtReouvertureReserveeAdmin() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    changerStatut(techToken, id, "PRET", 200);
    changerStatut(techToken, id, "CLOTURE", 200);

    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/pieces"), Map.of("reference", "SSD")))
        .andExpect(status().isConflict());
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/priorite"), Map.of("priorite", "HAUTE")))
        .andExpect(status().isConflict());
    // Une note reste possible
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/notes"), Map.of("contenu", "Client rappelé")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.auteur").value("Frere"));

    changerStatut(techToken, id, "DIAGNOSTIC", 403);
    changerStatut(adminToken, id, "DIAGNOSTIC", 200);
    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.closedAt").doesNotExist())
        .andExpect(jsonPath("$.evenements[0].typeAction").value("REOUVERTURE"));
  }

  @Test
  void blocageEmpecheLaClotureEtExigeUneRaison() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/blocage"), Map.of("bloque", true)))
        .andExpect(status().isBadRequest());
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/blocage"),
            Map.of("bloque", true, "raisonBlocage", "Paiement en attente")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloque").value(true));

    changerStatut(techToken, id, "PRET", 200);
    changerStatut(techToken, id, "CLOTURE", 409);

    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/blocage"), Map.of("bloque", false)))
        .andExpect(status().isOk());
    changerStatut(techToken, id, "CLOTURE", 200);
  }

  @Test
  void assignerPuisDesassignerUnTechnicien() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/technicien"), Map.of("technicienId", admin.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.technicien.prenom").value("Zak"));
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/technicien"), new HashMap<String, Object>()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.technicien").doesNotExist());
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/technicien"), Map.of("technicienId", 999)))
        .andExpect(status().isNotFound());
  }

  @Test
  void modifierLesInformationsDuDossier() throws Exception {
    long id = creerDossier("Martin Dubois", "0470123456");
    LocalDate dateEstimee = LocalDate.now().plusDays(3);
    mvc.perform(json(techToken, put("/api/dossiers/" + id), Map.of(
            "appareil", Map.of("type", "TOUR", "marque", "HP"),
            "descriptionProbleme", "Écran bleu",
            "dateEstimee", dateEstimee.toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.typeAppareil").value("TOUR"))
        .andExpect(jsonPath("$.dateEstimee").value(dateEstimee.toString()));

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.descriptionProbleme").value("Écran bleu"))
        .andExpect(jsonPath("$.accessoires").doesNotExist())
        .andExpect(jsonPath("$.appareil.modele").doesNotExist())
        .andExpect(jsonPath("$.evenements[0].typeAction").value("MODIF_DOSSIER"));

    mvc.perform(json(techToken, put("/api/dossiers/" + id), Map.of(
            "appareil", Map.of("type", "TOUR"),
            "dateEstimee", LocalDate.now().minusDays(10).toString())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechercheEtStatistiques() throws Exception {
    long a = creerDossier("Martin Dubois", "0470123456");
    creerDossier("Sophie Laurent", "0480999888");
    long c = creerDossier("Pierre Moreau", "0490111222");
    changerStatut(techToken, c, "PRET", 200);
    changerStatut(techToken, c, "CLOTURE", 200);
    mvc.perform(json(techToken, patch("/api/dossiers/" + a + "/blocage"),
        Map.of("bloque", true, "raisonBlocage", "Attente accord"))).andExpect(status().isOk());

    mvc.perform(as(techToken, get("/api/dossiers").param("q", "sophie")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].clientNom").value("Sophie Laurent"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
    mvc.perform(as(techToken, get("/api/dossiers").param("q", "0470 12")))
        .andExpect(jsonPath("$.content", hasSize(1)));
    mvc.perform(as(techToken, get("/api/dossiers").param("actifs", "true")))
        .andExpect(jsonPath("$.content", hasSize(2)));
    mvc.perform(as(techToken, get("/api/dossiers").param("bloque", "true")))
        .andExpect(jsonPath("$.content[0].id").value(a));
    mvc.perform(as(techToken, get("/api/dossiers").param("sort", "client.nom")))
        .andExpect(status().isBadRequest());

    String stats = mvc.perform(as(techToken, get("/api/dossiers/stats")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(((Number) read(stats, "$.actifs")).intValue()).isEqualTo(2);
    assertThat(((Number) read(stats, "$.bloques")).intValue()).isEqualTo(1);
    assertThat(((Number) read(stats, "$.parStatut.RECU")).intValue()).isEqualTo(2);
    assertThat(((Number) read(stats, "$.parStatut.CLOTURE")).intValue()).isEqualTo(1);
  }

  @Test
  void mesDossiersFiltreSurLEmployeConnecte() throws Exception {
    long a = creerDossier("Martin Dubois", "0470123456");
    long b = creerDossier("Sophie Laurent", "0480999888");
    creerDossier("Pierre Moreau", "0490111222");
    mvc.perform(json(techToken, patch("/api/dossiers/" + a + "/technicien"), Map.of("technicienId", tech.getId())))
        .andExpect(status().isOk());
    mvc.perform(json(techToken, patch("/api/dossiers/" + b + "/technicien"), Map.of("technicienId", admin.getId())))
        .andExpect(status().isOk());

    mvc.perform(as(techToken, get("/api/dossiers").param("mesDossiers", "true")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(a));
    mvc.perform(as(adminToken, get("/api/dossiers").param("mesDossiers", "true")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(b));
    mvc.perform(as(techToken, get("/api/dossiers/stats")))
        .andExpect(jsonPath("$.mesDossiers").value(1));

    changerStatut(techToken, a, "PRET", 200);
    changerStatut(techToken, a, "CLOTURE", 200);
    mvc.perform(as(techToken, get("/api/dossiers/stats")))
        .andExpect(jsonPath("$.mesDossiers").value(0));
  }

  @Test
  void dossierInexistantEn404() throws Exception {
    mvc.perform(as(techToken, get("/api/dossiers/12345")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}

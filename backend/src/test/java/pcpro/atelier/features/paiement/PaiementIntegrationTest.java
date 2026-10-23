package pcpro.atelier.features.paiement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

/**
 * Encaissement (à la clôture ou en acompte), caisse et garantie.
 */
class PaiementIntegrationTest extends IntegrationTest {

  private static final LocalDate AUJOURDHUI = LocalDate.now(ZoneId.of("Europe/Brussels"));

  private long dossierPret(String numeroSerie) throws Exception {
    Map<String, Object> body = Fixtures.nouveauDossier("Martin Dubois", "0470123456");
    body.put("appareil", Map.of("type", "PORTABLE", "marque", "Lenovo", "modele", "T14", "numeroSerie", numeroSerie));
    long id = creerDossier(body);
    changerStatut(techToken, id, "PRET", 200);
    return id;
  }

  @Test
  void clotureAvecPaiementEtGarantieParDefaut() throws Exception {
    long id = dossierPret("SN-1");
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of(
            "statut", "CLOTURE",
            "paiement", Map.of("montant", "149.90", "moyen", "BANCONTACT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.finGarantie").value(AUJOURDHUI.plusMonths(3).toString()));

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.paiements[0].montant").value(149.9))
        .andExpect(jsonPath("$.paiements[0].moyen").value("BANCONTACT"))
        .andExpect(jsonPath("$.paiements[0].employe").value("Frere"))
        .andExpect(jsonPath("$.totalPaye").value(149.9));
  }

  @Test
  void garantieChoisieALaClotureEtSupprimeeALaReouverture() throws Exception {
    long id = dossierPret("SN-1");
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of("statut", "CLOTURE", "garantieMois", 0)))
        .andExpect(jsonPath("$.finGarantie").doesNotExist());

    changerStatut(adminToken, id, "DIAGNOSTIC", 200);
    changerStatut(techToken, id, "PRET", 200);
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of("statut", "CLOTURE", "garantieMois", 12)))
        .andExpect(jsonPath("$.finGarantie").value(AUJOURDHUI.plusMonths(12).toString()));
    changerStatut(adminToken, id, "DIAGNOSTIC", 200);
    mvc.perform(as(techToken, get("/api/dossiers/" + id))).andExpect(jsonPath("$.finGarantie").doesNotExist());
  }

  @Test
  void paiementEtGarantieSeulementALaCloture() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of(
            "statut", "DIAGNOSTIC", "paiement", Map.of("montant", "10", "moyen", "ESPECES"))))
        .andExpect(status().isBadRequest());
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of(
            "statut", "PRET", "paiement", Map.of("montant", "0", "moyen", "ESPECES"))))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void acompteEtResteAPayerAvecDevisAccepte() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"),
        Map.of("lignes", List.of(Fixtures.ligneDevis("MAIN_OEUVRE", "Réparation", "120"))))).andExpect(status().isOk());
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"), Map.of("accepte", true)));

    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/paiements"), Map.of("montant", "50", "moyen", "ESPECES")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.montant").value(50));

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.totalPaye").value(50))
        .andExpect(jsonPath("$.resteAPayer").value(70.0));
  }

  @Test
  void caisseDuJourEtAnnulationReserveesAuxAdmins() throws Exception {
    long a = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    long b = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    String paiement = mvc.perform(json(techToken, post("/api/dossiers/" + a + "/paiements"),
            Map.of("montant", "30", "moyen", "ESPECES")))
        .andReturn().getResponse().getContentAsString();
    mvc.perform(json(techToken, post("/api/dossiers/" + b + "/paiements"), Map.of("montant", "45.50", "moyen", "BANCONTACT")));
    mvc.perform(json(techToken, post("/api/dossiers/" + b + "/paiements"), Map.of("montant", "4.50", "moyen", "BANCONTACT")));

    mvc.perform(as(techToken, get("/api/caisse"))).andExpect(status().isForbidden());
    mvc.perform(as(adminToken, get("/api/caisse")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.du").value(AUJOURDHUI.toString()))
        .andExpect(jsonPath("$.total").value(80.0))
        .andExpect(jsonPath("$.parMoyen.ESPECES").value(30))
        .andExpect(jsonPath("$.parMoyen.BANCONTACT").value(50.0))
        .andExpect(jsonPath("$.parMoyen.VIREMENT").value(0))
        .andExpect(jsonPath("$.paiements.length()").value(3))
        .andExpect(jsonPath("$.paiements[0].clientNom").value("Martin Dubois"));
    mvc.perform(as(adminToken, get("/api/caisse").param("du", AUJOURDHUI.minusDays(10).toString())
            .param("au", AUJOURDHUI.minusDays(1).toString())))
        .andExpect(jsonPath("$.total").value(0));

    long paiementId = ((Number) read(paiement, "$.id")).longValue();
    mvc.perform(as(techToken, delete("/api/dossiers/" + a + "/paiements/" + paiementId))).andExpect(status().isForbidden());
    mvc.perform(as(adminToken, delete("/api/dossiers/" + b + "/paiements/" + paiementId))).andExpect(status().isNotFound());
    mvc.perform(as(adminToken, delete("/api/dossiers/" + a + "/paiements/" + paiementId))).andExpect(status().isNoContent());
    mvc.perform(as(adminToken, get("/api/caisse"))).andExpect(jsonPath("$.total").value(50.0));
  }

  @Test
  void garantieRetrouveeQuandLAppareilRevient() throws Exception {
    long ancien = dossierPret("SN-ABC-1");
    changerStatut(techToken, ancien, "CLOTURE", 200);

    // Même appareil (n° de série saisi en minuscules), quelques semaines plus tard
    Map<String, Object> body = Fixtures.nouveauDossier("Martin Dubois", "0470123456");
    body.put("appareil", Map.of("type", "PORTABLE", "marque", "Lenovo", "modele", "T14", "numeroSerie", "sn-abc-1"));
    long nouveau = creerDossier(body);

    mvc.perform(as(techToken, get("/api/dossiers/" + nouveau)))
        .andExpect(jsonPath("$.garanties.length()").value(1))
        .andExpect(jsonPath("$.garanties[0].dossierId").value(ancien))
        .andExpect(jsonPath("$.garanties[0].finGarantie").value(AUJOURDHUI.plusMonths(3).toString()));
    mvc.perform(as(techToken, get("/api/dossiers/" + ancien))).andExpect(jsonPath("$.garanties.length()").value(0));

    mvc.perform(as(techToken, get("/api/dossiers/garanties").param("numeroSerie", " SN-abc-1 ")))
        .andExpect(jsonPath("$.length()").value(1));
    String client = mvc.perform(as(techToken, get("/api/dossiers/" + ancien))).andReturn().getResponse().getContentAsString();
    mvc.perform(as(techToken, get("/api/dossiers/garanties").param("clientId", String.valueOf(((Number) read(client, "$.client.id")).longValue()))))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(as(techToken, get("/api/dossiers/garanties").param("numeroSerie", "AUTRE")))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void chiffreAffairesParReparationRendueEtExportCsv() throws Exception {
    long rendu = dossierPret("SN-1");
    mvc.perform(json(techToken, post("/api/dossiers/" + rendu + "/pieces"),
        Map.of("reference", "SSD 500 Go", "cout", "45.10", "etat", "MONTEE"))).andExpect(status().isCreated());
    mvc.perform(json(techToken, post("/api/dossiers/" + rendu + "/pieces"),
        Map.of("reference", "Clavier", "cout", "20", "etat", "A_COMMANDER"))).andExpect(status().isCreated());
    String annulee = mvc.perform(json(techToken, post("/api/dossiers/" + rendu + "/pieces"),
        Map.of("reference", "Écran", "cout", "99", "etat", "A_COMMANDER"))).andReturn().getResponse().getContentAsString();
    mvc.perform(json(techToken, patch("/api/dossiers/" + rendu + "/pieces/" + ((Number) read(annulee, "$.id")).longValue() + "/etat"),
        Map.of("nouvelEtat", "ANNULEE"))).andExpect(status().isOk());
    mvc.perform(json(techToken, post("/api/dossiers/" + rendu + "/paiements"), Map.of("montant", "50", "moyen", "ESPECES")));
    mvc.perform(json(techToken, patch("/api/dossiers/" + rendu + "/statut"), Map.of(
        "statut", "CLOTURE", "paiement", Map.of("montant", "100", "moyen", "BANCONTACT")))).andExpect(status().isOk());

    // Encaissé mais pas encore rendu : pas dans le chiffre d'affaires
    long enCours = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    mvc.perform(json(techToken, post("/api/dossiers/" + enCours + "/paiements"), Map.of("montant", "30", "moyen", "ESPECES")));

    mvc.perform(as(techToken, get("/api/chiffre-affaires"))).andExpect(status().isForbidden());
    mvc.perform(as(adminToken, get("/api/chiffre-affaires")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reparations").value(1))
        .andExpect(jsonPath("$.encaisse").value(150.0))
        .andExpect(jsonPath("$.coutPieces").value(65.1))
        .andExpect(jsonPath("$.marge").value(84.9))
        .andExpect(jsonPath("$.lignes[0].clientNom").value("Martin Dubois"))
        .andExpect(jsonPath("$.lignes[0].appareil").value("ordinateur portable Lenovo T14"));
    mvc.perform(as(adminToken, get("/api/chiffre-affaires").param("au", AUJOURDHUI.minusDays(1).toString())
            .param("du", AUJOURDHUI.minusDays(30).toString())))
        .andExpect(jsonPath("$.reparations").value(0))
        .andExpect(jsonPath("$.marge").value(0));

    String csv = mvc.perform(as(adminToken, get("/api/chiffre-affaires/export")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThat(csv)
        .startsWith("\uFEFFRendu le;Référence;Client")
        .contains(";Martin Dubois;ordinateur portable Lenovo T14;;150,00;65,10;84,90\r\n")
        .contains("Total;;1 réparations;;;150,00;65,10;84,90");
  }
}

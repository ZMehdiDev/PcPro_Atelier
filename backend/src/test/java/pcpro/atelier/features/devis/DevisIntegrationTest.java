package pcpro.atelier.features.devis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.mail.internet.MimeMessage;
import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class DevisIntegrationTest extends IntegrationTest {

  private static final Map<String, Object> DEVIS = Map.of(
      "lignes", List.of(
          Map.of("type", "MAIN_OEUVRE", "libelle", "Remplacement écran", "quantite", "1.5", "prixUnitaire", "40"),
          Fixtures.ligneDevis("PIECE", "Écran 14 pouces", "89.90")),
      "commentaire", "Délai : 3 jours après accord");

  @Test
  void devisCalculeLeTotalEtMetLeDossierEnAttenteDuClient() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));

    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
        .andExpect(jsonPath("$.lignes[0].total").value(60.0))
        .andExpect(jsonPath("$.total").value(149.9));

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.statut").value("ATTENTE_CLIENT"))
        .andExpect(jsonPath("$.devis.total").value(149.9))
        .andExpect(jsonPath("$.evenements[1].typeAction").value("DEVIS"));
  }

  @Test
  void devisAccepteRelanceLaReparationEtNePeutPlusEtreModifie() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS)).andExpect(status().isOk());

    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"),
            Map.of("accepte", true, "commentaire", "Accord par téléphone")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("ACCEPTE"))
        .andExpect(jsonPath("$.decideAt").isNotEmpty());

    mvc.perform(as(techToken, get("/api/dossiers/" + id)))
        .andExpect(jsonPath("$.statut").value("REPARATION"))
        .andExpect(jsonPath("$.resteAPayer").value(149.9));

    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS)).andExpect(status().isConflict());
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"), Map.of("accepte", false)))
        .andExpect(status().isConflict());
    mvc.perform(as(techToken, delete("/api/dossiers/" + id + "/devis"))).andExpect(status().isConflict());
  }

  @Test
  void devisRefusePeutEtreRefaitPuisSupprime() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS)).andExpect(status().isOk());
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"), Map.of("accepte", false)))
        .andExpect(jsonPath("$.statut").value("REFUSE"));

    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"),
            Map.of("lignes", List.of(Fixtures.ligneDevis("MAIN_OEUVRE", "Réparation simple", "50")))))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
        .andExpect(jsonPath("$.lignes.length()").value(1))
        .andExpect(jsonPath("$.total").value(50.0));

    mvc.perform(as(techToken, delete("/api/dossiers/" + id + "/devis"))).andExpect(status().isNoContent());
    mvc.perform(as(techToken, get("/api/dossiers/" + id))).andExpect(jsonPath("$.devis").doesNotExist());
  }

  @Test
  void devisInvalideRefuse() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), Map.of("lignes", List.of())))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"),
            Map.of("lignes", List.of(Fixtures.ligneDevis("PIECE", "SSD", "-5")))))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"), Map.of("accepte", true)))
        .andExpect(status().isNotFound());
  }

  @Test
  void devisEnvoyeParEmailAvecLeLienDeSuivi() throws Exception {
    long id = creerDossierClientAvecEmail();
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS)).andExpect(status().isOk());

    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/devis/envoi"))).andExpect(status().isNoContent());

    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(envoi.capture());
    MimeMessage message = envoi.getValue();
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("martin@example.be");
    assertThat(message.getSubject()).startsWith("Devis pour votre ordinateur portable Lenovo ThinkPad T14");
    message.saveChanges();
    java.io.ByteArrayOutputStream contenu = new java.io.ByteArrayOutputStream();
    message.writeTo(contenu);
    assertThat(contenu.toString(java.nio.charset.StandardCharsets.UTF_8)).contains("https://atelier.test/suivi/");
  }

  @Test
  void envoiDuDevisSansEmailClientRefuse() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Martin Dubois", "0470123456"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), DEVIS)).andExpect(status().isOk());
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/devis/envoi")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Le client n'a pas d'adresse email : ajoutez-la sur sa fiche"));
    verify(mailSender, never()).send(any(MimeMessage.class));
  }
}

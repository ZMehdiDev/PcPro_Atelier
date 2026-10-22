package pcpro.atelier.features.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

import jakarta.mail.internet.MimeMessage;
import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

/**
 * Email "c'est prêt", appareils non récupérés et relance.
 */
class NotificationIntegrationTest extends IntegrationTest {

  private long dossierAvecEmail() throws Exception {
    return creerDossierClientAvecEmail();
  }

  @Test
  void bonDeDepotEnvoyeParEmailALaCreation() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossierAvecEmail("Martin Dubois", "0470123456", "martin@example.be"));

    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender, timeout(5000)).send(envoi.capture());
    MimeMessage message = envoi.getValue();
    assertThat(message.getSubject()).startsWith("Dépôt de votre ordinateur portable Lenovo ThinkPad T14 (PCP-");
    message.saveChanges();
    java.io.ByteArrayOutputStream contenu = new java.io.ByteArrayOutputStream();
    message.writeTo(contenu);
    assertThat(contenu.toString(java.nio.charset.StandardCharsets.UTF_8))
        .contains("application/pdf")
        .contains("bon-depot-PCP-");
    attendreHistorique(id, "Email : bon de dépôt");
  }

  @Test
  void pasDeBonParEmailSansAdresse() throws Exception {
    creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    verify(mailSender, after(500).never()).send(any(MimeMessage.class));
  }

  @Test
  void clientPrevenuParEmailQuandSonAppareilEstPret() throws Exception {
    long id = dossierAvecEmail();
    changerStatut(techToken, id, "PRET", 200);

    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender, timeout(5000)).send(envoi.capture());
    assertThat(envoi.getValue().getSubject()).isEqualTo("Votre ordinateur portable Lenovo ThinkPad T14 est prêt - Pc Pro Test");
    assertThat(envoi.getValue().getAllRecipients()[0].toString()).isEqualTo("martin@example.be");

    String detail = attendreHistorique(id, "Email : appareil prêt");
    assertThat((String) read(detail, "$.evenements[0].nouvelleValeur")).isEqualTo("Email : appareil prêt");
    assertThat((String) read(detail, "$.evenements[0].employePrenom")).isEqualTo("Frere");
  }

  @Test
  void pasDEmailSiDemandeOuSansAdresse() throws Exception {
    long avecEmail = dossierAvecEmail();
    mvc.perform(json(techToken, patch("/api/dossiers/" + avecEmail + "/statut"),
        Map.of("statut", "PRET", "notifierClient", false))).andExpect(status().isOk());
    long sansEmail = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    changerStatut(techToken, sansEmail, "PRET", 200);

    verify(mailSender, after(500).never()).send(any(MimeMessage.class));
  }

  @Test
  void echecDEnvoiNoteDansLHistorique() throws Exception {
    long id = dossierAvecEmail();
    doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(MimeMessage.class));
    changerStatut(techToken, id, "PRET", 200);

    String detail = attendreHistorique(id, "Échec de l'email : appareil prêt");
    assertThat((String) read(detail, "$.evenements[0].nouvelleValeur")).isEqualTo("Échec de l'email : appareil prêt");
    assertThat((String) read(detail, "$.statut")).isEqualTo("PRET");
  }

  @Test
  void appareilsPretsNonRecuperesEtRelance() throws Exception {
    long ancien = dossierAvecEmail();
    long recent = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    mvc.perform(json(techToken, patch("/api/dossiers/" + ancien + "/statut"), Map.of("statut", "PRET", "notifierClient", false)));
    changerStatut(techToken, recent, "PRET", 200);
    jdbcClient.sql("UPDATE dossier SET pret_at = pret_at - INTERVAL '10 days' WHERE id = :id").param("id", ancien).update();

    mvc.perform(as(techToken, get("/api/dossiers").param("nonRecuperes", "true")))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(ancien))
        .andExpect(jsonPath("$.content[0].nonRecupere").value(true));
    mvc.perform(as(techToken, get("/api/dossiers/stats"))).andExpect(jsonPath("$.nonRecuperes").value(1));

    mvc.perform(as(techToken, post("/api/dossiers/" + ancien + "/relance"))).andExpect(status().isNoContent());
    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(envoi.capture());
    assertThat(envoi.getValue().getSubject()).startsWith("Votre ordinateur portable Lenovo ThinkPad T14 vous attend");

    // Récupéré : il sort de la liste
    changerStatut(techToken, ancien, "CLOTURE", 200);
    mvc.perform(as(techToken, get("/api/dossiers/stats"))).andExpect(jsonPath("$.nonRecuperes").value(0));
  }

  @Test
  void relanceImpossibleSiPasPretOuSansEmail() throws Exception {
    long pasPret = dossierAvecEmail();
    mvc.perform(as(techToken, post("/api/dossiers/" + pasPret + "/relance"))).andExpect(status().isConflict());

    long sansEmail = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    changerStatut(techToken, sansEmail, "PRET", 200);
    mvc.perform(as(techToken, post("/api/dossiers/" + sansEmail + "/relance"))).andExpect(status().isBadRequest());
  }

  @Test
  void relanceQuandLEnvoiEchoueRepond503() throws Exception {
    long id = dossierAvecEmail();
    mvc.perform(json(techToken, patch("/api/dossiers/" + id + "/statut"), Map.of("statut", "PRET", "notifierClient", false)));
    doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(MimeMessage.class));

    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/relance")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("L'email n'a pas pu être envoyé, réessayez plus tard"));
  }
}

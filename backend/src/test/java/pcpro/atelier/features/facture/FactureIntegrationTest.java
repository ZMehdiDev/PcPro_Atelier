package pcpro.atelier.features.facture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import jakarta.mail.internet.MimeMessage;
import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class FactureIntegrationTest extends IntegrationTest {

  private final String annee = String.valueOf(Year.now().getValue());

  /** Dossier avec un devis accepté de 120 + 2 × 35 = 190 €. */
  private long dossierAvecDevisAccepte() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossierAvecEmail("Martin Dubois", "0470123456", "martin@example.be"));
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"), Map.of("lignes", List.of(
            Fixtures.ligneDevis("MAIN_OEUVRE", "Remplacement de l'écran", "120"),
            Map.of("type", "PIECE", "libelle", "Vis et nappe", "quantite", "2", "prixUnitaire", "35")))))
        .andExpect(status().isOk());
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/devis/decision"), Map.of("accepte", true)))
        .andExpect(status().isOk());
    return id;
  }

  private byte[] pdf(String token, String url) throws Exception {
    byte[] pdf = mvc.perform(as(token, get(url)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andReturn().getResponse().getContentAsByteArray();
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    return pdf;
  }

  @Test
  void factureDuDossierDApresLeDevisAccepte() throws Exception {
    long id = dossierAvecDevisAccepte();
    mvc.perform(as(techToken, get("/api/dossiers/" + id + "/facture"))).andExpect(status().isNoContent());

    String facture = mvc.perform(json(techToken, post("/api/dossiers/" + id + "/facture"),
            Map.of("clientAdresse", "Rue de la Loi 16, 1000 Bruxelles")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numero").value(annee + "-0001"))
        .andExpect(jsonPath("$.type").value("FACTURE"))
        .andExpect(jsonPath("$.total").value(190.0))
        .andExpect(jsonPath("$.clientNom").value("Martin Dubois"))
        .andExpect(jsonPath("$.clientEmail").value("martin@example.be"))
        .andExpect(jsonPath("$.clientAdresse").value("Rue de la Loi 16, 1000 Bruxelles"))
        .andExpect(jsonPath("$.dossierId").value(id))
        .andReturn().getResponse().getContentAsString();
    long factureId = ((Number) read(facture, "$.id")).longValue();

    // Redemander la facture renvoie la même (pas de doublon, pas de trou dans la numérotation)
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(factureId));
    mvc.perform(as(techToken, get("/api/dossiers/" + id + "/facture")))
        .andExpect(jsonPath("$.numero").value(annee + "-0001"));

    // Mentions obligatoires : numéro, BCE, client et adresse, régime de TVA (franchise par défaut)
    try (PDDocument document = Loader.loadPDF(pdf(techToken, "/api/factures/" + factureId + "/pdf"))) {
      assertThat(new PDFTextStripper().getText(document)).contains(
          annee + "-0001", "BCE 0895.968.808", "Martin Dubois", "Rue de la Loi 16, 1000 Bruxelles",
          "Remplacement de l'écran", "190,00", "Régime particulier de franchise des petites entreprises");
    }
    attendreHistorique(id, "Facture " + annee + "-0001");
  }

  @Test
  void sansDevisLaFactureReprendLeMontantEncaisse() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossier("Sophie Laurent", "0480999888"));
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture"))).andExpect(status().isBadRequest());

    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/paiements"), Map.of("montant", "45", "moyen", "BANCONTACT")))
        .andExpect(status().isCreated());
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(45.0));
  }

  @Test
  void noteDeCreditPuisNouvelleFacture() throws Exception {
    long id = dossierAvecDevisAccepte();
    String facture = mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")))
        .andReturn().getResponse().getContentAsString();
    long factureId = ((Number) read(facture, "$.id")).longValue();

    mvc.perform(as(techToken, post("/api/factures/" + factureId + "/annulation"))).andExpect(status().isForbidden());
    String note = mvc.perform(as(adminToken, post("/api/factures/" + factureId + "/annulation")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("NOTE_CREDIT"))
        .andExpect(jsonPath("$.numero").value("NC" + annee + "-0001"))
        .andExpect(jsonPath("$.total").value(-190.0))
        .andExpect(jsonPath("$.factureOrigine").value(annee + "-0001"))
        .andReturn().getResponse().getContentAsString();
    mvc.perform(as(adminToken, post("/api/factures/" + factureId + "/annulation"))).andExpect(status().isConflict());
    mvc.perform(as(adminToken, post("/api/factures/" + read(note, "$.id") + "/annulation"))).andExpect(status().isConflict());

    mvc.perform(as(techToken, get("/api/dossiers/" + id + "/facture"))).andExpect(status().isNoContent());
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")))
        .andExpect(jsonPath("$.numero").value(annee + "-0002"));

    mvc.perform(as(techToken, get("/api/factures/" + factureId)))
        .andExpect(jsonPath("$.annuleePar").value("NC" + annee + "-0001"));
    pdf(techToken, "/api/factures/" + factureId + "/pdf");
    pdf(techToken, "/api/factures/" + read(note, "$.id") + "/pdf");

    mvc.perform(as(techToken, get("/api/factures"))).andExpect(status().isForbidden());
    mvc.perform(as(adminToken, get("/api/factures")))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].annuleePar").value("NC" + annee + "-0001"));
  }

  @Test
  void factureDUneVenteEtVenteFactureeIndeletable() throws Exception {
    String vente = mvc.perform(json(techToken, post("/api/ventes"), Map.of("moyen", "BANCONTACT", "lignes", List.of(
            Map.of("article", "Souris sans fil", "quantite", 2, "prixUnitaire", "19.90")))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    long venteId = ((Number) read(vente, "$.id")).longValue();

    mvc.perform(json(techToken, post("/api/ventes/" + venteId + "/facture"), Map.of())).andExpect(status().isBadRequest());
    String facture = mvc.perform(json(techToken, post("/api/ventes/" + venteId + "/facture"),
            Map.of("clientNom", "ASBL Les Ateliers", "clientTva", "BE 0123.456.789")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(39.8))
        .andExpect(jsonPath("$.venteId").value(venteId))
        .andReturn().getResponse().getContentAsString();
    mvc.perform(as(techToken, get("/api/ventes/" + venteId + "/facture"))).andExpect(jsonPath("$.clientNom").value("ASBL Les Ateliers"));
    pdf(techToken, "/api/factures/" + read(facture, "$.id") + "/pdf");

    mvc.perform(as(adminToken, delete("/api/ventes/" + venteId))).andExpect(status().isConflict());
  }

  @Test
  void factureEnvoyeeParEmailAvecLePdf() throws Exception {
    long id = dossierAvecDevisAccepte();
    String facture = mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")))
        .andReturn().getResponse().getContentAsString();
    attendreHistorique(id, "Email : bon de dépôt");
    org.mockito.Mockito.clearInvocations(mailSender);

    mvc.perform(as(techToken, post("/api/factures/" + read(facture, "$.id") + "/envoi"))).andExpect(status().isNoContent());
    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(envoi.capture());
    MimeMessage message = envoi.getValue();
    assertThat(message.getSubject()).startsWith("Facture " + annee + "-0001");
    message.saveChanges();
    ByteArrayOutputStream contenu = new ByteArrayOutputStream();
    message.writeTo(contenu);
    assertThat(contenu.toString(StandardCharsets.UTF_8)).contains("facture-" + annee + "-0001.pdf");
    attendreHistorique(id, "Email : facture " + annee + "-0001");
  }

  @Test
  void devisEnPdfEtJoint() throws Exception {
    long id = creerDossierClientAvecEmail();
    mvc.perform(as(techToken, get("/api/dossiers/" + id + "/devis/pdf"))).andExpect(status().isNotFound());
    mvc.perform(json(techToken, put("/api/dossiers/" + id + "/devis"),
        Map.of("lignes", List.of(Fixtures.ligneDevis("MAIN_OEUVRE", "Diagnostic et réparation", "80"))))).andExpect(status().isOk());
    pdf(techToken, "/api/dossiers/" + id + "/devis/pdf");

    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/devis/envoi"))).andExpect(status().isNoContent());
    ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(envoi.capture());
    MimeMessage message = envoi.getValue();
    message.saveChanges();
    ByteArrayOutputStream contenu = new ByteArrayOutputStream();
    message.writeTo(contenu);
    assertThat(contenu.toString(StandardCharsets.UTF_8)).contains("devis-PCP-").contains("application/pdf");
  }

  @Test
  void journalDesRecettesReserveAuxAdmins() throws Exception {
    long id = dossierAvecDevisAccepte();
    mvc.perform(json(techToken, post("/api/dossiers/" + id + "/paiements"), Map.of("montant", "50", "moyen", "ESPECES")));
    mvc.perform(as(techToken, post("/api/dossiers/" + id + "/facture")));

    mvc.perform(as(techToken, get("/api/factures/journal"))).andExpect(status().isForbidden());
    pdf(adminToken, "/api/factures/journal");
    mvc.perform(as(adminToken, get("/api/factures/journal").param("du", "2026-02-01").param("au", "2026-01-01")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void documentsProteges() throws Exception {
    mvc.perform(get("/api/factures/1/pdf")).andExpect(status().isUnauthorized());
    mvc.perform(as(techToken, get("/api/factures/999/pdf"))).andExpect(status().isNotFound());
    mvc.perform(post("/api/dossiers/1/facture")).andExpect(status().isUnauthorized());
  }
}

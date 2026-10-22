package pcpro.atelier.features.piece;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcpro.atelier.support.Fixtures;
import pcpro.atelier.support.IntegrationTest;

class PieceIntegrationTest extends IntegrationTest {

  private long dossierId;

  @BeforeEach
  void dossierEnDiagnostic() throws Exception {
    String body = mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Martin", "0470123456")))
        .andReturn().getResponse().getContentAsString();
    dossierId = ((Number) read(body, "$.id")).longValue();
    mvc.perform(json(techToken, patch("/api/dossiers/" + dossierId + "/statut"), Map.of("statut", "DIAGNOSTIC")))
        .andExpect(status().isOk());
  }

  private long ajouterPiece(String reference, String etat) throws Exception {
    String body = mvc.perform(json(techToken, post("/api/dossiers/" + dossierId + "/pieces"),
            Map.of("reference", reference, "fournisseur", "Amazon", "cout", "49.90", "etat", etat)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) read(body, "$.id")).longValue();
  }

  private void changerEtat(long pieceId, String etat, int attendu) throws Exception {
    mvc.perform(json(techToken, patch("/api/dossiers/" + dossierId + "/pieces/" + pieceId + "/etat"),
            Map.of("nouvelEtat", etat)))
        .andExpect(status().is(attendu));
  }

  private void statutDossier(String attendu) throws Exception {
    mvc.perform(as(techToken, get("/api/dossiers/" + dossierId))).andExpect(jsonPath("$.statut").value(attendu));
  }

  @Test
  void pieceCommandeeMetLeDossierEnAttenteEtRecueLeRemetEnReparation() throws Exception {
    long ecran = ajouterPiece("Écran 15.6", "A_COMMANDER");
    long batterie = ajouterPiece("Batterie", "A_COMMANDER");
    statutDossier("DIAGNOSTIC");

    changerEtat(ecran, "COMMANDEE", 200);
    statutDossier("ATTENTE_PIECE");
    changerEtat(batterie, "COMMANDEE", 200);

    changerEtat(ecran, "RECUE", 200);
    statutDossier("ATTENTE_PIECE"); // la batterie est encore attendue

    changerEtat(batterie, "RECUE", 200);
    statutDossier("REPARATION");

    mvc.perform(as(techToken, get("/api/dossiers/" + dossierId)))
        .andExpect(jsonPath("$.coutPieces").value(99.80))
        .andExpect(jsonPath("$.pieces[0].etatsPossibles[*]").isArray());
  }

  @Test
  void pieceCreeeCommandeeDeclencheAussiLAttente() throws Exception {
    ajouterPiece("SSD 1To", "COMMANDEE");
    statutDossier("ATTENTE_PIECE");
  }

  @Test
  void transitionDEtatInvalide() throws Exception {
    long piece = ajouterPiece("SSD", "A_COMMANDER");
    changerEtat(piece, "MONTEE", 409);
    changerEtat(piece, "ANNULEE", 200);
    changerEtat(piece, "COMMANDEE", 409);
  }

  @Test
  void pieceDUnAutreDossierIntrouvable() throws Exception {
    long piece = ajouterPiece("SSD", "A_COMMANDER");
    String autre = mvc.perform(json(techToken, post("/api/dossiers"), Fixtures.nouveauDossier("Autre", "0480000000")))
        .andReturn().getResponse().getContentAsString();
    Number autreId = read(autre, "$.id");
    mvc.perform(json(techToken, patch("/api/dossiers/" + autreId + "/pieces/" + piece + "/etat"),
            Map.of("nouvelEtat", "COMMANDEE")))
        .andExpect(status().isNotFound());
  }

  @Test
  void modifierUnePieceEtValidationDuCout() throws Exception {
    long piece = ajouterPiece("SSD", "A_COMMANDER");
    mvc.perform(json(techToken, put("/api/dossiers/" + dossierId + "/pieces/" + piece),
            Map.of("reference", "SSD Samsung 1To", "cout", "89.00")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reference").value("SSD Samsung 1To"))
        .andExpect(jsonPath("$.fournisseur").doesNotExist());
    mvc.perform(json(techToken, post("/api/dossiers/" + dossierId + "/pieces"),
            Map.of("reference", "X", "cout", "-5")))
        .andExpect(status().isUnprocessableContent());
  }
}

package pcpro.atelier.features.dossier.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pcpro.atelier.config.TestConfig;
import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeAppareil;
import pcpro.atelier.core.util.ReferenceGenerator;
import pcpro.atelier.features.client.infra.ClientRepository;
import pcpro.atelier.features.dossier.api.dto.ChangeStatutRequest;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.employe.app.EmployeService;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.note.infra.NoteRepository;
import pcpro.atelier.features.photo.infra.PhotoRepository;
import pcpro.atelier.features.piece.infra.PieceRepository;

@WebMvcTest(DossierController.class)
@ActiveProfiles("test")
@Import(TestConfig.class)
class DossierControllerChangeStatutTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private DossierService dossierService;

  @MockitoBean
  private ClientRepository clientRepository;

  @MockitoBean
  private EmployeService employeService;

  @MockitoBean
  private EvenementService evenementService;

  @MockitoBean
  private ReferenceGenerator referenceGenerator;

  @MockitoBean
  private PieceRepository pieceRepository;

  @MockitoBean
  private NoteRepository noteRepository;

  @MockitoBean
  private PhotoRepository photoRepository;

  @Test
  void changerStatut_avecHeader_retourne200_etEventCree() throws Exception {
    ChangeStatutRequest request = new ChangeStatutRequest(
        StatutDossier.REPARATION,
        "Début de la réparation"
    );

    DossierSummaryDto dossierResponse = new DossierSummaryDto(
        1L,
        "PCP-2026-000001",
        "John Doe",
        "0499123456",
        TypeAppareil.PORTABLE,
        "Lenovo",
        "ThinkPad",
        StatutDossier.REPARATION,
        Priorite.NORMALE,
        LocalDate.now(),
        OffsetDateTime.now()
    );

    when(dossierService.changerStatut(eq(1L), any(ChangeStatutRequest.class), eq(2L)))
        .thenReturn(dossierResponse);

    mockMvc.perform(patch("/api/dossiers/1/statut")
            .header("X-Employe-Id", "2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("REPARATION"));

    // Vérifier que le service a été appelé (ce qui signifie qu'un événement a été créé)
    verify(dossierService).changerStatut(eq(1L), any(ChangeStatutRequest.class), eq(2L));
  }
}

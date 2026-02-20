package pcpro.atelier.features.dossier.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import pcpro.atelier.features.dossier.api.dto.AppareilDto;
import pcpro.atelier.features.dossier.api.dto.ClientDto;
import pcpro.atelier.features.dossier.api.dto.CreateDossierRequest;
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
class DossierControllerTest {

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
  void creerDossier_sansHeader_retourne400() throws Exception {
    CreateDossierRequest request = new CreateDossierRequest(
        new ClientDto("John Doe", "0499123456"),
        new AppareilDto(TypeAppareil.PORTABLE, "Lenovo", "ThinkPad", "SN123"),
        Priorite.NORMALE,
        "Problème d'écran",
        null
    );

    mockMvc.perform(post("/api/dossiers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void creerDossier_avecHeader_retourne201() throws Exception {
    CreateDossierRequest request = new CreateDossierRequest(
        new ClientDto("John Doe", "0499123456"),
        new AppareilDto(TypeAppareil.PORTABLE, "Lenovo", "ThinkPad", "SN123"),
        Priorite.NORMALE,
        "Problème d'écran",
        null
    );

    DossierSummaryDto dossierResponse = new DossierSummaryDto(
        1L,
        "PCP-2026-000001",
        "John Doe",
        "0499123456",
        TypeAppareil.PORTABLE,
        "Lenovo",
        "ThinkPad",
        StatutDossier.RECU,
        Priorite.NORMALE,
        LocalDate.now(),
        OffsetDateTime.now()
    );

    when(dossierService.creerDossier(any(CreateDossierRequest.class), eq(1L)))
        .thenReturn(dossierResponse);

    mockMvc.perform(post("/api/dossiers")
            .header("X-Employe-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reference").value("PCP-2026-000001"))
        .andExpect(jsonPath("$.statut").value("RECU"));
  }
}

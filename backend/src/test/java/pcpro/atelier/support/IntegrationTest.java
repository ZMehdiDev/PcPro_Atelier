package pcpro.atelier.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import pcpro.atelier.core.enums.Role;
import pcpro.atelier.core.security.LoginAttemptService;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Base des tests d'intégration : application complète + PostgreSQL (Testcontainers) + MockMvc.
 * Chaque test démarre sur une base vide avec deux comptes : admin (ADMIN) et tech (TECHNICIEN).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

  protected static final String ADMIN_PASSWORD = "admin-password";
  protected static final String TECH_PASSWORD = "tech-password";

  @Autowired
  protected MockMvc mvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected EmployeRepository employeRepository;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  @Autowired
  protected JdbcClient jdbcClient;

  @Autowired
  private LoginAttemptService loginAttemptService;

  /** Aucun email ne part pendant les tests : on vérifie les envois sur ce mock. */
  @MockitoBean
  protected JavaMailSender mailSender;

  protected Employe admin;
  protected Employe tech;
  protected String adminToken;
  protected String techToken;

  @BeforeEach
  void resetDatabase() throws Exception {
    jdbcClient.sql("""
        TRUNCATE evenement, note, photo, piece, devis_ligne, devis, paiement, vente_ligne, vente, dossier, client, reference_compteur, session_employe, employe
        RESTART IDENTITY CASCADE
        """).update();
    loginAttemptService.reinitialiser();
    when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
    admin = creerEmploye("Zak", "admin", ADMIN_PASSWORD, Role.ADMIN);
    tech = creerEmploye("Frere", "tech", TECH_PASSWORD, Role.TECHNICIEN);
    adminToken = login("admin", ADMIN_PASSWORD);
    techToken = login("tech", TECH_PASSWORD);
  }

  protected Employe creerEmploye(String prenom, String login, String motDePasse, Role role) {
    Employe employe = new Employe();
    employe.setPrenom(prenom);
    employe.setLogin(login);
    employe.setPasswordHash(passwordEncoder.encode(motDePasse));
    employe.setRole(role);
    return employeRepository.save(employe);
  }

  protected String login(String login, String motDePasse) throws Exception {
    String body = mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("login", login, "motDePasse", motDePasse))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.token");
  }

  /** Crée un dossier (en tant que technicien) et renvoie son id. */
  protected long creerDossier(Map<String, Object> body) throws Exception {
    String reponse = mvc.perform(json(techToken, post("/api/dossiers"), body))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) read(reponse, "$.id")).longValue();
  }

  /**
   * Crée un dossier pour un client qui a un email, attend l'envoi automatique du bon de dépôt
   * puis oublie cet envoi (les tests vérifient les emails suivants).
   */
  protected long creerDossierClientAvecEmail() throws Exception {
    long id = creerDossier(Fixtures.nouveauDossierAvecEmail("Martin Dubois", "0470123456", "martin@example.be"));
    attendreHistorique(id, "Email : bon de dépôt");
    clearInvocations(mailSender);
    return id;
  }

  /** Les emails partent en arrière-plan : attend que l'historique du dossier contienne ce texte. */
  protected String attendreHistorique(long id, String texte) throws Exception {
    for (int i = 0; i < 50; i++) {
      String detail = mvc.perform(as(techToken, get("/api/dossiers/" + id))).andReturn().getResponse().getContentAsString();
      if (detail.contains(texte)) {
        return detail;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("L'historique du dossier " + id + " ne contient pas : " + texte);
  }

  protected void changerStatut(String token, long id, String statut, int attendu) throws Exception {
    mvc.perform(json(token, patch("/api/dossiers/" + id + "/statut"), Map.of("statut", statut)))
        .andExpect(status().is(attendu));
  }

  /** Ajoute le token Bearer à une requête. */
  protected static <B extends AbstractMockHttpServletRequestBuilder<B>> B as(String token, B request) {
    return request.header("Authorization", "Bearer " + token);
  }

  /** Requête JSON authentifiée. */
  protected MockHttpServletRequestBuilder json(String token, MockHttpServletRequestBuilder request, Object body) {
    return as(token, request).contentType(MediaType.APPLICATION_JSON).content(json(body));
  }

  protected String json(Object body) {
    return objectMapper.writeValueAsString(body);
  }

  protected static <T> T read(String json, String path) {
    return JsonPath.read(json, path);
  }
}

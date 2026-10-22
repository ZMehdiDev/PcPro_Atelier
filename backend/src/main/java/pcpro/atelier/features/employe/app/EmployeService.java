package pcpro.atelier.features.employe.app;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.employe.api.dto.CreateEmployeRequest;
import pcpro.atelier.features.employe.api.dto.EmployeDto;
import pcpro.atelier.features.employe.api.dto.UpdateEmployeRequest;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

@Service
@Transactional
public class EmployeService {

  private final EmployeRepository employeRepository;
  private final PasswordEncoder passwordEncoder;
  private final CurrentEmploye currentEmploye;
  private final Clock clock;

  public EmployeService(
      EmployeRepository employeRepository,
      PasswordEncoder passwordEncoder,
      CurrentEmploye currentEmploye,
      Clock clock
  ) {
    this.employeRepository = employeRepository;
    this.passwordEncoder = passwordEncoder;
    this.currentEmploye = currentEmploye;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<EmployeDto> lister(boolean inclureInactifs) {
    List<Employe> employes = inclureInactifs
        ? employeRepository.findAllByOrderByPrenomAsc()
        : employeRepository.findByActifTrueOrderByPrenomAsc();
    return employes.stream().map(EmployeService::toDto).toList();
  }

  @Transactional(readOnly = true)
  public Employe findByIdAndActifOrThrow(Long id) {
    Employe employe = employeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
    if (!employe.isActif()) {
      throw new ConflictException("L'employé " + employe.getPrenom() + " est désactivé");
    }
    return employe;
  }

  public EmployeDto creer(CreateEmployeRequest request) {
    String login = normaliserLogin(request.login());
    if (employeRepository.existsByLogin(login)) {
      throw new ConflictException("Le login '" + login + "' est déjà utilisé");
    }
    Employe employe = new Employe();
    employe.setPrenom(request.prenom().trim());
    employe.setLogin(login);
    employe.setRole(request.role());
    employe.setPasswordHash(encoderMotDePasse(request.motDePasse()));
    employe.setActif(true);
    return toDto(employeRepository.save(employe));
  }

  public EmployeDto modifier(Long id, UpdateEmployeRequest request) {
    Employe employe = employeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

    // Garde-fou : un admin ne peut pas se retirer ses propres droits (risque de verrouiller l'atelier)
    if (employe.getId().equals(currentEmploye.id())
        && (!request.actif() || request.role() != employe.getRole())) {
      throw new ConflictException("Vous ne pouvez pas modifier votre propre rôle ni vous désactiver");
    }

    employe.setPrenom(request.prenom().trim());
    employe.setRole(request.role());
    if (employe.isActif() && !request.actif()) {
      employe.setCredentialsChangedAt(OffsetDateTime.now(clock));
    }
    employe.setActif(request.actif());
    return toDto(employe);
  }

  public void reinitialiserMotDePasse(Long id, String motDePasse) {
    Employe employe = employeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
    definirMotDePasse(employe, motDePasse);
  }

  /**
   * Change le mot de passe et révoque les tokens existants de cet employé.
   */
  public void definirMotDePasse(Employe employe, String motDePasse) {
    employe.setPasswordHash(encoderMotDePasse(motDePasse));
    employe.setCredentialsChangedAt(OffsetDateTime.now(clock));
  }

  /** bcrypt ignore tout au-delà de 72 octets : on refuse plutôt que de tronquer en silence. */
  private String encoderMotDePasse(String motDePasse) {
    if (motDePasse.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new BadRequestException("Le mot de passe est trop long (72 octets maximum)");
    }
    return passwordEncoder.encode(motDePasse);
  }

  public static String normaliserLogin(String login) {
    return login == null ? null : login.trim().toLowerCase(Locale.ROOT);
  }

  public static EmployeDto toDto(Employe employe) {
    return new EmployeDto(
        employe.getId(),
        employe.getPrenom(),
        employe.getLogin(),
        employe.getRole(),
        employe.isActif(),
        employe.getPasswordHash() != null,
        employe.getCreatedAt()
    );
  }
}

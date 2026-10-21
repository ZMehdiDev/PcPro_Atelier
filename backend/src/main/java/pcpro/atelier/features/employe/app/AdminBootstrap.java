package pcpro.atelier.features.employe.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.BootstrapProperties;
import pcpro.atelier.core.enums.Role;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

/**
 * Au démarrage, si APP_BOOTSTRAP_ADMIN_LOGIN / APP_BOOTSTRAP_ADMIN_PASSWORD sont définis :
 * - crée ce compte admin s'il n'existe pas ;
 * - ou lui donne ce mot de passe et le rôle admin s'il existe sans mot de passe.
 * Un compte qui a déjà un mot de passe n'est jamais modifié.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final BootstrapProperties properties;
  private final EmployeRepository employeRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminBootstrap(
      BootstrapProperties properties,
      EmployeRepository employeRepository,
      PasswordEncoder passwordEncoder
  ) {
    this.properties = properties;
    this.employeRepository = employeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.estConfigure()) {
      return;
    }
    if (properties.adminPassword().length() < 8) {
      log.warn("Bootstrap admin ignoré : le mot de passe doit faire au moins 8 caractères");
      return;
    }

    String login = EmployeService.normaliserLogin(properties.adminLogin());
    Employe employe = employeRepository.findByLogin(login).orElse(null);

    if (employe == null) {
      employe = new Employe();
      employe.setLogin(login);
      employe.setPrenom(properties.adminPrenom());
    } else if (employe.getPasswordHash() != null) {
      return; // déjà initialisé
    }

    employe.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
    employe.setRole(Role.ADMIN);
    employe.setActif(true);
    employeRepository.save(employe);
    log.info("Compte administrateur '{}' initialisé", login);
  }
}

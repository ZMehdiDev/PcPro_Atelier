package pcpro.atelier.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("!test & !dev")
public class FlywayConfig {

  private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

  @Autowired
  private DataSource dataSource;

  @Bean
  public Flyway flyway() {
    log.info("=== Initializing Flyway migration ===");
    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load();
    
    log.info("Running Flyway migrations...");
    var result = flyway.migrate();
    log.info("=== Flyway migrations completed: {} migrations applied ===", result.migrationsExecuted);
    
    return flyway;
  }
}

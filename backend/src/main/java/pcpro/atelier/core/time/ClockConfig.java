package pcpro.atelier.core.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  /**
   * Horloge de l'application, dans le fuseau de l'atelier (dates d'entrée, retards...).
   */
  @Bean
  public Clock clock(@Value("${app.timezone:Europe/Brussels}") String timezone) {
    return Clock.system(ZoneId.of(timezone));
  }
}

package pcpro.atelier.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Tâches en arrière-plan (emails envoyés sans faire attendre l'employé). */
@Configuration
@EnableAsync
public class AsyncConfig {
}

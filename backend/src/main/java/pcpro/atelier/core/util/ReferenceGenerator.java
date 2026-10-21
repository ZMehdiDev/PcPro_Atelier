package pcpro.atelier.core.util;

import java.time.Clock;
import java.time.Year;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Génère les références de dossier au format PCP-YYYY-NNNNNN (ex : PCP-2026-000001).
 *
 * Le compteur annuel est incrémenté atomiquement en base (upsert), donc deux dossiers créés
 * en même temps ne peuvent pas obtenir la même référence.
 */
@Component
public class ReferenceGenerator {

  private final JdbcClient jdbcClient;
  private final Clock clock;

  public ReferenceGenerator(JdbcClient jdbcClient, Clock clock) {
    this.jdbcClient = jdbcClient;
    this.clock = clock;
  }

  public String generateReference() {
    int annee = Year.now(clock).getValue();
    int numero = jdbcClient.sql("""
        INSERT INTO reference_compteur (annee, dernier) VALUES (:annee, 1)
        ON CONFLICT (annee) DO UPDATE SET dernier = reference_compteur.dernier + 1
        RETURNING dernier
        """)
        .param("annee", annee)
        .query(Integer.class)
        .single();
    return String.format("PCP-%d-%06d", annee, numero);
  }
}

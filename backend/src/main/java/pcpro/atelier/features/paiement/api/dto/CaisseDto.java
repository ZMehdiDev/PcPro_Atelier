package pcpro.atelier.features.paiement.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import pcpro.atelier.core.enums.MoyenPaiement;

/**
 * Encaissements d'une période (bornes incluses).
 *
 * @param parMoyen total par moyen de paiement (tous les moyens sont présents)
 */
public record CaisseDto(
    LocalDate du,
    LocalDate au,
    BigDecimal total,
    Map<MoyenPaiement, BigDecimal> parMoyen,
    List<Ligne> paiements
) {

  public record Ligne(
      Long id,
      Long dossierId,
      String reference,
      String clientNom,
      BigDecimal montant,
      MoyenPaiement moyen,
      String employe,
      OffsetDateTime createdAt
  ) {}
}

package pcpro.atelier.features.paiement.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.features.paiement.domain.Paiement;

public record PaiementDto(
    Long id,
    BigDecimal montant,
    MoyenPaiement moyen,
    String employe,
    OffsetDateTime createdAt
) {

  public static PaiementDto of(Paiement paiement) {
    return new PaiementDto(paiement.getId(), paiement.getMontant(), paiement.getMoyen(),
        paiement.getEmploye().getPrenom(), paiement.getCreatedAt());
  }
}

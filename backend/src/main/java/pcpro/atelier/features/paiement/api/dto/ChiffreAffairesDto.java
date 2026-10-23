package pcpro.atelier.features.paiement.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Chiffre d'affaires des réparations rendues sur une période (bornes incluses) : ce que chaque
 * réparation a rapporté, ce que ses pièces ont coûté et la marge.
 *
 * @param encaisse total des paiements des dossiers rendus sur la période (acomptes compris)
 * @param coutPieces total du coût des pièces non annulées de ces dossiers
 */
public record ChiffreAffairesDto(
    LocalDate du,
    LocalDate au,
    int reparations,
    BigDecimal encaisse,
    BigDecimal coutPieces,
    BigDecimal marge,
    List<Ligne> lignes
) {
  public record Ligne(
      Long dossierId,
      String reference,
      OffsetDateTime renduLe,
      String clientNom,
      String appareil,
      String technicien,
      BigDecimal encaisse,
      BigDecimal coutPieces,
      BigDecimal marge
  ) {}
}

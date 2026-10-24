package pcpro.atelier.features.paiement.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Chiffre d'affaires d'une période (bornes incluses) : les réparations rendues (ce que chacune a
 * rapporté, ce que ses pièces ont coûté, la marge), puis les ventes au comptoir, puis le total.
 *
 * @param reparations nombre de réparations rendues
 * @param encaisse total des paiements des dossiers rendus sur la période (acomptes compris)
 * @param coutPieces total du coût des pièces non annulées de ces dossiers
 * @param marge marge des réparations
 */
public record ChiffreAffairesDto(
    LocalDate du,
    LocalDate au,
    int reparations,
    BigDecimal encaisse,
    BigDecimal coutPieces,
    BigDecimal marge,
    List<Ligne> lignes,
    Ventes ventes,
    Total total
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

  /** @param cout prix d'achat des articles (ceux dont on l'a saisi) */
  public record Ventes(int nombre, BigDecimal encaisse, BigDecimal cout, BigDecimal marge, List<LigneVente> lignes) {}

  public record LigneVente(
      Long venteId,
      OffsetDateTime venduLe,
      String article,
      int quantite,
      String employe,
      BigDecimal encaisse,
      BigDecimal cout,
      BigDecimal marge
  ) {}

  /** Réparations et ventes ensemble. */
  public record Total(BigDecimal encaisse, BigDecimal cout, BigDecimal marge) {}
}

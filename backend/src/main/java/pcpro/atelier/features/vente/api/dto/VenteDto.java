package pcpro.atelier.features.vente.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.features.vente.domain.Vente;

public record VenteDto(
    Long id,
    MoyenPaiement moyen,
    BigDecimal total,
    String employe,
    OffsetDateTime createdAt,
    List<Ligne> lignes
) {

  public record Ligne(String article, int quantite, BigDecimal prixUnitaire, BigDecimal coutUnitaire) {}

  public static VenteDto of(Vente vente) {
    return new VenteDto(vente.getId(), vente.getMoyen(), vente.total(), vente.getEmploye().getPrenom(),
        vente.getCreatedAt(), vente.getLignes().stream()
            .map(l -> new Ligne(l.getArticle(), l.getQuantite(), l.getPrixUnitaire(), l.getCoutUnitaire()))
            .toList());
  }

  /** Ex : « 2 × Protection d'écran, Écouteurs ». */
  public static String resume(Vente vente) {
    return String.join(", ", vente.getLignes().stream()
        .map(l -> (l.getQuantite() > 1 ? l.getQuantite() + " × " : "") + l.getArticle())
        .toList());
  }
}

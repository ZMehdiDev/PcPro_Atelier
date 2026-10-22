package pcpro.atelier.features.devis.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.features.devis.domain.Devis;

/**
 * @param total total TTC
 */
public record DevisDto(
    StatutDevis statut,
    List<LigneDevisDto> lignes,
    BigDecimal total,
    String commentaire,
    OffsetDateTime updatedAt,
    OffsetDateTime decideAt
) {

  public static DevisDto of(Devis devis) {
    return devis == null ? null : new DevisDto(
        devis.getStatut(),
        devis.getLignes().stream()
            .map(l -> new LigneDevisDto(l.getType(), l.getLibelle(), l.getQuantite(), l.getPrixUnitaire(), l.total()))
            .toList(),
        devis.total(),
        devis.getCommentaire(),
        devis.getUpdatedAt(),
        devis.getDecideAt()
    );
  }
}

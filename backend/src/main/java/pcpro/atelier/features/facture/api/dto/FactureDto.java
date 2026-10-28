package pcpro.atelier.features.facture.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import pcpro.atelier.core.enums.TypeFacture;
import pcpro.atelier.features.facture.domain.Facture;

/**
 * @param total               total TVAC (négatif pour une note de crédit)
 * @param annuleePar          numéro de la note de crédit qui annule cette facture, sinon null
 * @param factureOrigine      pour une note de crédit : numéro de la facture annulée
 */
public record FactureDto(
    Long id,
    TypeFacture type,
    String numero,
    LocalDate dateEmission,
    Long dossierId,
    String dossierReference,
    Long venteId,
    String clientNom,
    String clientAdresse,
    String clientTva,
    String clientEmail,
    BigDecimal total,
    String annuleePar,
    String factureOrigine,
    OffsetDateTime createdAt
) {

  public static FactureDto of(Facture facture, String annuleePar) {
    return new FactureDto(
        facture.getId(),
        facture.getType(),
        facture.getNumero(),
        facture.getDateEmission(),
        facture.getDossier() != null ? facture.getDossier().getId() : null,
        facture.getDossier() != null ? facture.getDossier().getReference() : null,
        facture.getVente() != null ? facture.getVente().getId() : null,
        facture.getClientNom(),
        facture.getClientAdresse(),
        facture.getClientTva(),
        facture.getClientEmail(),
        facture.getTotal(),
        annuleePar,
        facture.getFactureOrigine() != null ? facture.getFactureOrigine().getNumero() : null,
        facture.getCreatedAt()
    );
  }
}

package pcpro.atelier.features.devis.api.dto;

import java.math.BigDecimal;

import pcpro.atelier.core.enums.TypeLigneDevis;

public record LigneDevisDto(
    TypeLigneDevis type,
    String libelle,
    BigDecimal quantite,
    BigDecimal prixUnitaire,
    BigDecimal total
) {}

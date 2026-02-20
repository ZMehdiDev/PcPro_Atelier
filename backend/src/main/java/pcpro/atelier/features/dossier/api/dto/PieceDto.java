package pcpro.atelier.features.dossier.api.dto;

import pcpro.atelier.core.enums.EtatPiece;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PieceDto(
    Long id,
    String reference,
    String fournisseur,
    BigDecimal cout,
    EtatPiece etat,
    OffsetDateTime createdAt
) {}

package pcpro.atelier.features.piece.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import pcpro.atelier.core.enums.EtatPiece;

public record PieceDto(
    Long id,
    String reference,
    String fournisseur,
    BigDecimal cout,
    EtatPiece etat,
    /** États vers lesquels la pièce peut passer (pour le front). */
    Set<EtatPiece> etatsPossibles,
    OffsetDateTime createdAt
) {}

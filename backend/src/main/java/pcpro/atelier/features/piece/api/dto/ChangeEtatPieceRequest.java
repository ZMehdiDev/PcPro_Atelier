package pcpro.atelier.features.piece.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.EtatPiece;

public record ChangeEtatPieceRequest(
    @NotNull(message = "Le nouvel état de la pièce est obligatoire")
    EtatPiece nouvelEtat,

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String commentaire
) {}

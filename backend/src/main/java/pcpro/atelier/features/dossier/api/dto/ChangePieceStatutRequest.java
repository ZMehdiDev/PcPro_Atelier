package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;
import pcpro.atelier.core.enums.EtatPiece;

public record ChangePieceStatutRequest(
    @NotNull(message = "Le nouvel état de la pièce est obligatoire")
    EtatPiece nouvelEtat,
    
    String commentaire
) {}

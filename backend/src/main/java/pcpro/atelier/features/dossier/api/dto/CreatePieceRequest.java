package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.EtatPiece;

import java.math.BigDecimal;

public record CreatePieceRequest(
    @NotBlank(message = "La référence de la pièce est obligatoire")
    @Size(max = 120, message = "La référence ne peut pas dépasser 120 caractères")
    String reference,
    
    @Size(max = 120, message = "Le fournisseur ne peut pas dépasser 120 caractères")
    String fournisseur,
    
    BigDecimal cout,
    
    @NotNull(message = "L'état de la pièce est obligatoire")
    EtatPiece etat
) {}

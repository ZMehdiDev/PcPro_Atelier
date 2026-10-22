package pcpro.atelier.features.piece.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.EtatPiece;

/**
 * etat est optionnel (A_COMMANDER par défaut). MONTEE permet d'enregistrer une pièce prise en stock.
 */
public record CreatePieceRequest(
    @NotBlank(message = "La référence de la pièce est obligatoire")
    @Size(max = 120, message = "La référence ne peut pas dépasser 120 caractères")
    String reference,

    @Size(max = 120, message = "Le fournisseur ne peut pas dépasser 120 caractères")
    String fournisseur,

    @DecimalMin(value = "0.00", message = "Le coût ne peut pas être négatif")
    @Digits(integer = 8, fraction = 2, message = "Le coût doit avoir au plus 2 décimales")
    BigDecimal cout,

    EtatPiece etat
) {}

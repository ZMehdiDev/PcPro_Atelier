package pcpro.atelier.features.devis.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.TypeLigneDevis;

/**
 * @param quantite facultative, 1 par défaut (ex : 1,5 pour 1 h 30 de main-d'œuvre)
 */
public record LigneDevisRequest(
    @NotNull(message = "Le type de ligne est obligatoire")
    TypeLigneDevis type,

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 200, message = "Le libellé ne peut pas dépasser 200 caractères")
    String libelle,

    @DecimalMin(value = "0.01", message = "La quantité doit être positive")
    @Digits(integer = 6, fraction = 2, message = "Quantité invalide")
    BigDecimal quantite,

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0", message = "Le prix ne peut pas être négatif")
    @Digits(integer = 8, fraction = 2, message = "Prix invalide (2 décimales maximum)")
    BigDecimal prixUnitaire
) {}

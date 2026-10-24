package pcpro.atelier.features.vente.api.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pcpro.atelier.core.enums.MoyenPaiement;

public record VenteRequest(
    @NotNull(message = "Le moyen de paiement est obligatoire")
    MoyenPaiement moyen,

    @NotEmpty(message = "Ajoutez au moins un article")
    @Size(max = 50, message = "50 articles maximum par vente")
    List<@Valid Ligne> lignes
) {

  public record Ligne(
      @NotBlank(message = "Le nom de l'article est obligatoire")
      @Size(max = 200, message = "Nom d'article trop long (200 caractères maximum)")
      String article,

      @Min(value = 1, message = "La quantité doit être d'au moins 1")
      @Max(value = 999, message = "Quantité trop grande")
      int quantite,

      @NotNull(message = "Le prix est obligatoire")
      @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
      @Digits(integer = 8, fraction = 2, message = "Prix invalide (2 décimales maximum)")
      BigDecimal prixUnitaire,

      @DecimalMin(value = "0.00", message = "Le prix d'achat ne peut pas être négatif")
      @Digits(integer = 8, fraction = 2, message = "Prix d'achat invalide (2 décimales maximum)")
      BigDecimal coutUnitaire
  ) {}
}

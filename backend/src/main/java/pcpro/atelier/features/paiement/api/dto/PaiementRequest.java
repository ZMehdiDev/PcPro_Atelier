package pcpro.atelier.features.paiement.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import pcpro.atelier.core.enums.MoyenPaiement;

public record PaiementRequest(
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    @Digits(integer = 8, fraction = 2, message = "Montant invalide (2 décimales maximum)")
    BigDecimal montant,

    @NotNull(message = "Le moyen de paiement est obligatoire")
    MoyenPaiement moyen
) {}

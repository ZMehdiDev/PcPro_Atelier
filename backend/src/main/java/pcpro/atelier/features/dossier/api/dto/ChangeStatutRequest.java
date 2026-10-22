package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.paiement.api.dto.PaiementRequest;

/**
 * @param notifierClient vers PRET : prévenir le client par email (oui par défaut, s'il a un email)
 * @param garantieMois   à la clôture : durée de garantie (défaut de l'atelier si absent, 0 = aucune)
 * @param paiement       à la clôture : somme encaissée
 */
public record ChangeStatutRequest(
    @NotNull(message = "Le statut est obligatoire")
    StatutDossier statut,

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String commentaire,

    Boolean notifierClient,

    @Min(value = 0, message = "La garantie ne peut pas être négative")
    @Max(value = 60, message = "La garantie ne peut pas dépasser 60 mois")
    Integer garantieMois,

    @Valid
    PaiementRequest paiement
) {}

package pcpro.atelier.features.devis.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionDevisRequest(
    @NotNull(message = "Indiquez si le devis est accepté")
    Boolean accepte,

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String commentaire
) {}

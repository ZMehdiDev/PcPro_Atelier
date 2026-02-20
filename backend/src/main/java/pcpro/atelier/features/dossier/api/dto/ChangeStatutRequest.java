package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.StatutDossier;

public record ChangeStatutRequest(
    @NotNull(message = "Le statut est obligatoire")
    StatutDossier statut,
    
    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String commentaire
) {}

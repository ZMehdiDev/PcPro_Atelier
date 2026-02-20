package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;
import pcpro.atelier.core.enums.Priorite;

public record ChangePrioriteRequest(
    @NotNull(message = "La priorité est obligatoire")
    Priorite priorite
) {}

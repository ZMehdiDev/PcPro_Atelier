package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeTechnicienRequest(
    @NotNull(message = "L'ID du technicien est obligatoire")
    Long technicienId
) {}

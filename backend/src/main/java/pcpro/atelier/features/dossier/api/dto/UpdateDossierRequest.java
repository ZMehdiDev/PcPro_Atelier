package pcpro.atelier.features.dossier.api.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Remplace les informations descriptives du dossier (un champ absent est vidé).
 */
public record UpdateDossierRequest(
    @NotNull(message = "Les informations appareil sont obligatoires")
    @Valid
    AppareilDto appareil,

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    String descriptionProbleme,

    @Size(max = 255, message = "Les accessoires ne peuvent pas dépasser 255 caractères")
    String accessoires,

    LocalDate dateEstimee
) {}

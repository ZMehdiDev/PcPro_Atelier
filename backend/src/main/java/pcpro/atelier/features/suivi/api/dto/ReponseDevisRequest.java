package pcpro.atelier.features.suivi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReponseDevisRequest(
    @NotBlank(message = "Le code de suivi est obligatoire")
    String code,

    @NotNull(message = "Indiquez si vous acceptez le devis")
    Boolean accepte
) {}

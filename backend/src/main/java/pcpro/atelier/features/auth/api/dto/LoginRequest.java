package pcpro.atelier.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Le login est obligatoire")
    @Size(max = 50)
    String login,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(max = 72)
    String motDePasse
) {}

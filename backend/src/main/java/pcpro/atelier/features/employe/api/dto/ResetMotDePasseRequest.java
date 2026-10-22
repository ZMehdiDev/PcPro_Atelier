package pcpro.atelier.features.employe.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetMotDePasseRequest(
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
    String motDePasse
) {}

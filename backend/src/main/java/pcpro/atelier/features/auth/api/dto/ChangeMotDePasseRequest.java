package pcpro.atelier.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMotDePasseRequest(
    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    @Size(max = 72)
    String ancienMotDePasse,

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
    String nouveauMotDePasse
) {}

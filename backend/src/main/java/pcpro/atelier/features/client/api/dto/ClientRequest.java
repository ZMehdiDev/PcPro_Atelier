package pcpro.atelier.features.client.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
    String nom,

    @NotBlank(message = "Le téléphone est obligatoire")
    @Size(max = 32, message = "Le téléphone ne peut pas dépasser 32 caractères")
    String telephone,

    @Email(message = "L'email n'est pas valide")
    @Size(max = 255, message = "L'email ne peut pas dépasser 255 caractères")
    String email
) {}

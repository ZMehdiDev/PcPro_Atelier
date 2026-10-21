package pcpro.atelier.features.employe.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.Role;

public record CreateEmployeRequest(
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères")
    String prenom,

    @NotBlank(message = "Le login est obligatoire")
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "Le login doit faire 3 à 50 caractères (lettres, chiffres, . _ -)")
    String login,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
    String motDePasse,

    @NotNull(message = "Le rôle est obligatoire")
    Role role
) {}

package pcpro.atelier.features.employe.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.Role;

public record UpdateEmployeRequest(
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères")
    String prenom,

    @NotNull(message = "Le rôle est obligatoire")
    Role role,

    @NotNull(message = "Le champ actif est obligatoire")
    Boolean actif
) {}

package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDto(
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
    String nom,
    
    @NotBlank(message = "Le téléphone est obligatoire")
    @Size(max = 32, message = "Le téléphone ne peut pas dépasser 32 caractères")
    String telephone
) {}

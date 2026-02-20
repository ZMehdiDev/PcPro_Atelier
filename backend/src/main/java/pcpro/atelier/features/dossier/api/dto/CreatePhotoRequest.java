package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePhotoRequest(
    @NotBlank(message = "Le chemin du fichier est obligatoire")
    @Size(max = 500, message = "Le chemin ne peut pas dépasser 500 caractères")
    String cheminFichier,
    
    @Size(max = 200, message = "La description ne peut pas dépasser 200 caractères")
    String description
) {}

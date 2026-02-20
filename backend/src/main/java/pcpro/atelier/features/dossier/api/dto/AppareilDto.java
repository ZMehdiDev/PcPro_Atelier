package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.TypeAppareil;

public record AppareilDto(
    @NotNull(message = "Le type d'appareil est obligatoire")
    TypeAppareil type,
    
    @Size(max = 80, message = "La marque ne peut pas dépasser 80 caractères")
    String marque,
    
    @Size(max = 120, message = "Le modèle ne peut pas dépasser 120 caractères")
    String modele,
    
    @Size(max = 120, message = "Le numéro de série ne peut pas dépasser 120 caractères")
    String numeroSerie
) {}

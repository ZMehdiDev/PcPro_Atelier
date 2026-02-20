package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.Size;

public record ChangeBlocageRequest(
    boolean bloque,
    
    @Size(max = 200, message = "La raison de blocage ne peut pas dépasser 200 caractères")
    String raisonBlocage
) {}

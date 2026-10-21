package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeBlocageRequest(
    @NotNull(message = "Le champ bloque est obligatoire")
    Boolean bloque,

    @Size(max = 200, message = "La raison de blocage ne peut pas dépasser 200 caractères")
    String raisonBlocage
) {}

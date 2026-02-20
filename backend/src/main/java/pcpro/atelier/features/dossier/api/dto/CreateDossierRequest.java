package pcpro.atelier.features.dossier.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.Priorite;

public record CreateDossierRequest(
    @NotNull(message = "Les informations client sont obligatoires")
    @Valid
    ClientDto client,
    
    @NotNull(message = "Les informations appareil sont obligatoires")
    @Valid
    AppareilDto appareil,
    
    Priorite priorite,
    
    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    String descriptionProbleme,
    
    Long technicienAssigneId
) {}

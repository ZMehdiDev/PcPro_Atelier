package pcpro.atelier.features.dossier.api.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.features.client.api.dto.ClientRequest;

/**
 * Le client est soit un client existant (clientId), soit décrit (client), retrouvé par téléphone ou créé.
 */
public record CreateDossierRequest(
    Long clientId,

    @Valid
    ClientRequest client,

    @NotNull(message = "Les informations appareil sont obligatoires")
    @Valid
    AppareilDto appareil,

    Priorite priorite,

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    String descriptionProbleme,

    @Size(max = 255, message = "Les accessoires ne peuvent pas dépasser 255 caractères")
    String accessoires,

    LocalDate dateEstimee,

    Long technicienAssigneId
) {}

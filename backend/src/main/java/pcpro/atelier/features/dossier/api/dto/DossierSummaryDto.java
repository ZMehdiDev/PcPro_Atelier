package pcpro.atelier.features.dossier.api.dto;

import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeAppareil;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DossierSummaryDto(
    Long id,
    String reference,
    String clientNom,
    String clientTelephone,
    TypeAppareil typeAppareil,
    String marque,
    String modele,
    StatutDossier statut,
    Priorite priorite,
    LocalDate dateEntree,
    OffsetDateTime createdAt
) {}

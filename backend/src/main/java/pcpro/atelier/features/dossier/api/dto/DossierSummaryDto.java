package pcpro.atelier.features.dossier.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeAppareil;

public record DossierSummaryDto(
    Long id,
    String reference,
    Long clientId,
    String clientNom,
    String clientTelephone,
    TypeAppareil typeAppareil,
    String marque,
    String modele,
    StatutDossier statut,
    Priorite priorite,
    TechnicienDto technicien,
    LocalDate dateEntree,
    LocalDate dateEstimee,
    boolean enRetard,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt
) {}

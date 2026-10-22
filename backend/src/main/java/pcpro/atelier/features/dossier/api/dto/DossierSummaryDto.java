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
    /** Passage au statut PRET (null si pas prêt). */
    OffsetDateTime pretDepuis,
    /** Prêt depuis plus longtemps que le délai de récupération de l'atelier. */
    boolean nonRecupere,
    LocalDate finGarantie,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt
) {}

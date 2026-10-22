package pcpro.atelier.features.dossier.api.dto;

import java.util.Map;

import pcpro.atelier.core.enums.StatutDossier;

/**
 * Chiffres pour le tableau de bord de l'atelier.
 *
 * @param parStatut nombre de dossiers par statut (tous les statuts sont présents)
 * @param actifs    dossiers non clôturés
 * @param bloques   dossiers actifs bloqués
 * @param enRetard  dossiers actifs dont la date estimée est dépassée (hors PRET)
 * @param mesDossiers dossiers actifs assignés à l'employé connecté
 */
public record DossierStatsDto(
    Map<StatutDossier, Long> parStatut,
    long actifs,
    long bloques,
    long enRetard,
    long mesDossiers
) {}

package pcpro.atelier.features.dossier.api.dto;

import java.time.LocalDate;

/** Réparation précédente encore sous garantie. */
public record GarantieDto(
    Long dossierId,
    String reference,
    String marque,
    String modele,
    String numeroSerie,
    LocalDate finGarantie
) {}

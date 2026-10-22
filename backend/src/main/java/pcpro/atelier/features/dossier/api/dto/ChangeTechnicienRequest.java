package pcpro.atelier.features.dossier.api.dto;

/**
 * technicienId null = désassigner le dossier.
 */
public record ChangeTechnicienRequest(
    Long technicienId
) {}

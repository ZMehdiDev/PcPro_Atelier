package pcpro.atelier.features.dossier.api.dto;

import java.time.OffsetDateTime;

public record PhotoDto(
    Long id,
    String cheminFichier,
    String mimeType,
    Long tailleBytes,
    String description,
    OffsetDateTime createdAt
) {}

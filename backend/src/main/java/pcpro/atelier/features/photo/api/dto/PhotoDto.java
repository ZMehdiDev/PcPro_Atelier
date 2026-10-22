package pcpro.atelier.features.photo.api.dto;

import java.time.OffsetDateTime;

public record PhotoDto(
    Long id,
    /** URL (authentifiée) pour récupérer l'image. */
    String url,
    String mimeType,
    Long tailleBytes,
    String description,
    OffsetDateTime createdAt
) {}

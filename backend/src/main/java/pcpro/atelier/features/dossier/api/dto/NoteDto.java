package pcpro.atelier.features.dossier.api.dto;

import java.time.OffsetDateTime;

public record NoteDto(
    Long id,
    String contenu,
    boolean sensible,
    OffsetDateTime createdAt
) {}

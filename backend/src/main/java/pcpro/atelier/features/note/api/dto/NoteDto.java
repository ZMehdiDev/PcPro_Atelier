package pcpro.atelier.features.note.api.dto;

import java.time.OffsetDateTime;

public record NoteDto(
    Long id,
    String contenu,
    boolean sensible,
    /** Prénom de l'auteur (null pour les notes antérieures à son enregistrement). */
    String auteur,
    OffsetDateTime createdAt
) {}

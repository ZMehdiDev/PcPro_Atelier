package pcpro.atelier.features.dossier.api.dto;

import java.time.OffsetDateTime;

public record ClientInfoDto(
    Long id,
    String nom,
    String telephone,
    OffsetDateTime createdAt
) {}

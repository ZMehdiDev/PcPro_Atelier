package pcpro.atelier.features.client.api.dto;

import java.time.OffsetDateTime;

public record ClientDto(
    Long id,
    String nom,
    String telephone,
    String email,
    OffsetDateTime createdAt
) {}

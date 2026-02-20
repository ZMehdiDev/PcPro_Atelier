package pcpro.atelier.features.employe.api.dto;

import java.time.OffsetDateTime;

public record EmployeDto(
    Long id,
    String prenom,
    boolean actif,
    OffsetDateTime createdAt
) {}

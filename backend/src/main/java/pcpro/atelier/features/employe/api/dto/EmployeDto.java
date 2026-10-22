package pcpro.atelier.features.employe.api.dto;

import java.time.OffsetDateTime;

import pcpro.atelier.core.enums.Role;

public record EmployeDto(
    Long id,
    String prenom,
    String login,
    Role role,
    boolean actif,
    boolean motDePasseDefini,
    OffsetDateTime createdAt
) {}

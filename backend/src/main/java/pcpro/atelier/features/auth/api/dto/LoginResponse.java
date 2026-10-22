package pcpro.atelier.features.auth.api.dto;

import java.time.Instant;

import pcpro.atelier.features.employe.api.dto.EmployeDto;

public record LoginResponse(
    String token,
    Instant expireLe,
    EmployeDto employe
) {}

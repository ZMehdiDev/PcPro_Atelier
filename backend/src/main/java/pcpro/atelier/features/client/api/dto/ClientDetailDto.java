package pcpro.atelier.features.client.api.dto;

import java.util.List;

import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;

public record ClientDetailDto(
    ClientDto client,
    List<DossierSummaryDto> dossiers
) {}

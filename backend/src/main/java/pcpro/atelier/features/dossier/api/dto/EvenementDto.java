package pcpro.atelier.features.dossier.api.dto;

import pcpro.atelier.core.enums.TypeActionEvenement;

import java.time.OffsetDateTime;

public record EvenementDto(
    Long id,
    String employePrenom,
    TypeActionEvenement typeAction,
    String ancienneValeur,
    String nouvelleValeur,
    String commentaire,
    OffsetDateTime createdAt
) {}

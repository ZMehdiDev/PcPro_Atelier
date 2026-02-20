package pcpro.atelier.features.dossier.api.dto;

import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeAppareil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DossierDetailDto(
    Long id,
    String reference,
    ClientInfoDto client,
    TypeAppareil typeAppareil,
    String marque,
    String modele,
    String numeroSerie,
    StatutDossier statut,
    boolean bloque,
    String raisonBlocage,
    String technicienPrenom,
    Priorite priorite,
    LocalDate dateEntree,
    LocalDate dateEstimee,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt,
    List<PieceDto> pieces,
    List<NoteDto> notes,
    List<PhotoDto> photos,
    List<EvenementDto> evenements
) {}

package pcpro.atelier.features.dossier.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.client.api.dto.ClientDto;
import pcpro.atelier.features.note.api.dto.NoteDto;
import pcpro.atelier.features.photo.api.dto.PhotoDto;
import pcpro.atelier.features.piece.api.dto.PieceDto;

public record DossierDetailDto(
    Long id,
    String reference,
    ClientDto client,
    AppareilDto appareil,
    String descriptionProbleme,
    String accessoires,
    StatutDossier statut,
    /** Statuts vers lesquels le dossier peut passer (pour le front). */
    Set<StatutDossier> statutsPossibles,
    boolean bloque,
    String raisonBlocage,
    TechnicienDto technicien,
    Priorite priorite,
    LocalDate dateEntree,
    LocalDate dateEstimee,
    boolean enRetard,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt,
    /** Somme des coûts des pièces non annulées. */
    BigDecimal coutPieces,
    List<PieceDto> pieces,
    List<NoteDto> notes,
    List<PhotoDto> photos,
    List<EvenementDto> evenements
) {}

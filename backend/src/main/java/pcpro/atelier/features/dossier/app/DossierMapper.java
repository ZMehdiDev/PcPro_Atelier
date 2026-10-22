package pcpro.atelier.features.dossier.app;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.core.util.PieceStateTransitionValidator;
import pcpro.atelier.core.util.StatutDossierWorkflow;
import pcpro.atelier.features.client.app.ClientService;
import pcpro.atelier.features.dossier.api.dto.AppareilDto;
import pcpro.atelier.features.dossier.api.dto.DossierDetailDto;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.devis.api.dto.DevisDto;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.dossier.api.dto.EvenementDto;
import pcpro.atelier.features.dossier.api.dto.GarantieDto;
import pcpro.atelier.features.dossier.api.dto.TechnicienDto;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.evenement.domain.Evenement;
import pcpro.atelier.features.note.api.dto.NoteDto;
import pcpro.atelier.features.note.domain.Note;
import pcpro.atelier.features.paiement.api.dto.PaiementDto;
import pcpro.atelier.features.paiement.domain.Paiement;
import pcpro.atelier.features.photo.api.dto.PhotoDto;
import pcpro.atelier.features.photo.domain.Photo;
import pcpro.atelier.features.piece.api.dto.PieceDto;
import pcpro.atelier.features.piece.domain.Piece;

/**
 * Conversions entités -> DTO de l'API (centralisées pour que toutes les réponses soient cohérentes).
 */
@Component
public class DossierMapper {

  /** Données du détail qui ne viennent pas des pièces/notes/photos/historique. */
  public record Complements(
      Devis devis,
      List<Paiement> paiements,
      BigDecimal totalPaye,
      BigDecimal resteAPayer,
      List<GarantieDto> garanties
  ) {}

  private final Clock clock;
  private final AtelierProperties atelier;

  public DossierMapper(Clock clock, AtelierProperties atelier) {
    this.clock = clock;
    this.atelier = atelier;
  }

  private boolean nonRecupere(Dossier dossier) {
    return dossier.estNonRecupere(OffsetDateTime.now(clock).minus(atelier.delaiRecuperation()));
  }

  public DossierSummaryDto toSummaryDto(Dossier dossier) {
    return new DossierSummaryDto(
        dossier.getId(),
        dossier.getReference(),
        dossier.getClient().getId(),
        dossier.getClient().getNom(),
        dossier.getClient().getTelephone(),
        dossier.getTypeAppareil(),
        dossier.getMarque(),
        dossier.getModele(),
        dossier.getStatut(),
        dossier.getPriorite(),
        toTechnicienDto(dossier.getTechnicienAssigne()),
        dossier.getDateEntree(),
        dossier.getDateEstimee(),
        dossier.estEnRetard(LocalDate.now(clock)),
        dossier.getPretAt(),
        nonRecupere(dossier),
        dossier.getFinGarantie(),
        dossier.getCreatedAt(),
        dossier.getClosedAt()
    );
  }

  public DossierDetailDto toDetailDto(
      Dossier dossier,
      List<Piece> pieces,
      List<Note> notes,
      List<Photo> photos,
      List<Evenement> evenements,
      Complements complements
  ) {
    BigDecimal coutPieces = pieces.stream()
        .filter(p -> p.getEtat() != EtatPiece.ANNULEE)
        .map(Piece::getCout)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new DossierDetailDto(
        dossier.getId(),
        dossier.getReference(),
        ClientService.toDto(dossier.getClient()),
        new AppareilDto(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele(), dossier.getNumeroSerie()),
        dossier.getDescriptionProbleme(),
        dossier.getAccessoires(),
        dossier.getStatut(),
        StatutDossierWorkflow.transitionsPossibles(dossier.getStatut()),
        toTechnicienDto(dossier.getTechnicienAssigne()),
        dossier.getPriorite(),
        dossier.getDateEntree(),
        dossier.getDateEstimee(),
        dossier.estEnRetard(LocalDate.now(clock)),
        dossier.getPretAt(),
        nonRecupere(dossier),
        dossier.getCreatedAt(),
        dossier.getClosedAt(),
        dossier.getCodeSuivi(),
        atelier.lienSuivi(dossier.getReference(), dossier.getCodeSuivi()),
        dossier.getFinGarantie(),
        complements.garanties(),
        DevisDto.of(complements.devis()),
        complements.paiements().stream().map(PaiementDto::of).toList(),
        complements.totalPaye(),
        complements.resteAPayer(),
        coutPieces,
        pieces.stream().map(DossierMapper::toPieceDto).toList(),
        notes.stream().map(DossierMapper::toNoteDto).toList(),
        photos.stream().map(DossierMapper::toPhotoDto).toList(),
        evenements.stream().map(DossierMapper::toEvenementDto).toList()
    );
  }

  public static TechnicienDto toTechnicienDto(Employe employe) {
    return employe == null ? null : new TechnicienDto(employe.getId(), employe.getPrenom());
  }

  public static PieceDto toPieceDto(Piece piece) {
    return new PieceDto(
        piece.getId(),
        piece.getReference(),
        piece.getFournisseur(),
        piece.getCout(),
        piece.getEtat(),
        PieceStateTransitionValidator.transitionsPossibles(piece.getEtat()),
        piece.getCreatedAt()
    );
  }

  public static NoteDto toNoteDto(Note note) {
    return new NoteDto(
        note.getId(),
        note.getContenu(),
        note.isSensible(),
        note.getAuteur() != null ? note.getAuteur().getPrenom() : null,
        note.getCreatedAt()
    );
  }

  public static PhotoDto toPhotoDto(Photo photo) {
    return new PhotoDto(
        photo.getId(),
        "/api/dossiers/" + photo.getDossier().getId() + "/photos/" + photo.getId() + "/contenu",
        photo.getMimeType(),
        photo.getTailleBytes(),
        photo.getDescription(),
        photo.getCreatedAt()
    );
  }

  public static EvenementDto toEvenementDto(Evenement evenement) {
    return new EvenementDto(
        evenement.getId(),
        evenement.getEmploye() != null ? evenement.getEmploye().getPrenom() : "Client",
        evenement.getTypeAction(),
        evenement.getAncienneValeur(),
        evenement.getNouvelleValeur(),
        evenement.getCommentaire(),
        evenement.getCreatedAt()
    );
  }
}

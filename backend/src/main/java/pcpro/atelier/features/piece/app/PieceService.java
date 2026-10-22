package pcpro.atelier.features.piece.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.PieceStateTransitionValidator;
import pcpro.atelier.core.util.StatutDossierWorkflow;
import pcpro.atelier.features.dossier.app.DossierMapper;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.piece.api.dto.ChangeEtatPieceRequest;
import pcpro.atelier.features.piece.api.dto.CreatePieceRequest;
import pcpro.atelier.features.piece.api.dto.PieceDto;
import pcpro.atelier.features.piece.api.dto.UpdatePieceRequest;
import pcpro.atelier.features.piece.domain.Piece;
import pcpro.atelier.features.piece.infra.PieceRepository;

@Service
@Transactional
public class PieceService {

  /** Pièces dont l'atelier attend encore l'arrivée. */
  private static final EnumSet<EtatPiece> EN_ATTENTE = EnumSet.of(EtatPiece.A_COMMANDER, EtatPiece.COMMANDEE);

  private final DossierService dossierService;
  private final PieceRepository pieceRepository;
  private final EvenementService evenementService;

  public PieceService(
      DossierService dossierService,
      PieceRepository pieceRepository,
      EvenementService evenementService
  ) {
    this.dossierService = dossierService;
    this.pieceRepository = pieceRepository;
    this.evenementService = evenementService;
  }

  public PieceDto ajouter(Long dossierId, CreatePieceRequest request) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    EtatPiece etat = request.etat() != null ? request.etat() : EtatPiece.A_COMMANDER;
    if (etat == EtatPiece.ANNULEE) {
      throw new BadRequestException("Une pièce ne peut pas être créée à l'état ANNULEE");
    }

    Piece piece = new Piece();
    piece.setDossier(dossier);
    piece.setReference(request.reference().trim());
    piece.setFournisseur(texteOuNull(request.fournisseur()));
    piece.setCout(request.cout());
    piece.setEtat(etat);
    piece = pieceRepository.save(piece);

    evenementService.journaliser(dossier, TypeActionEvenement.AJOUT_PIECE, null,
        "Pièce ajoutée: " + piece.getReference() + " (" + etat + ")", null);

    if (etat == EtatPiece.COMMANDEE) {
      passerEnAttentePiece(dossier, piece);
    }
    return DossierMapper.toPieceDto(piece);
  }

  public PieceDto modifier(Long dossierId, Long pieceId, UpdatePieceRequest request) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Piece piece = getPieceDuDossier(dossier, pieceId);

    List<String> changements = new ArrayList<>();
    String reference = request.reference().trim();
    String fournisseur = texteOuNull(request.fournisseur());
    if (!reference.equals(piece.getReference())) {
      changements.add("Référence: " + piece.getReference() + " -> " + reference);
    }
    if (!Objects.equals(fournisseur, piece.getFournisseur())) {
      changements.add("Fournisseur: " + piece.getFournisseur() + " -> " + fournisseur);
    }
    if (!memeMontant(piece.getCout(), request.cout())) {
      changements.add("Coût: " + piece.getCout() + " -> " + request.cout());
    }
    if (changements.isEmpty()) {
      return DossierMapper.toPieceDto(piece);
    }

    piece.setReference(reference);
    piece.setFournisseur(fournisseur);
    piece.setCout(request.cout());
    evenementService.journaliser(dossier, TypeActionEvenement.MODIF_PIECE, null,
        String.join(", ", changements), null);
    return DossierMapper.toPieceDto(piece);
  }

  /**
   * Change l'état d'une pièce et fait suivre le dossier :
   * - pièce COMMANDEE : le dossier passe en ATTENTE_PIECE (si le workflow le permet) ;
   * - pièce RECUE et plus aucune pièce attendue : un dossier en ATTENTE_PIECE repasse en REPARATION.
   */
  public PieceDto changerEtat(Long dossierId, Long pieceId, ChangeEtatPieceRequest request) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Piece piece = getPieceDuDossier(dossier, pieceId);

    EtatPiece ancienEtat = piece.getEtat();
    EtatPiece nouvelEtat = request.nouvelEtat();
    PieceStateTransitionValidator.validerTransition(ancienEtat, nouvelEtat);
    piece.setEtat(nouvelEtat);
    pieceRepository.flush();

    evenementService.journaliser(
        dossier,
        TypeActionEvenement.CHANGEMENT_STATUT_PIECE,
        ancienEtat.toString(),
        nouvelEtat.toString(),
        request.commentaire() != null && !request.commentaire().isBlank()
            ? request.commentaire().trim()
            : "Pièce " + piece.getReference() + " : " + ancienEtat + " -> " + nouvelEtat
    );

    if (nouvelEtat == EtatPiece.COMMANDEE) {
      passerEnAttentePiece(dossier, piece);
    } else if (nouvelEtat == EtatPiece.RECUE
        && dossier.getStatut() == StatutDossier.ATTENTE_PIECE
        && !pieceRepository.existsByDossierAndEtatIn(dossier, EN_ATTENTE)) {
      dossierService.appliquerStatut(dossier, StatutDossier.REPARATION,
          "Automatique : toutes les pièces attendues sont arrivées (dernière : " + piece.getReference() + ")");
    }
    return DossierMapper.toPieceDto(piece);
  }

  private void passerEnAttentePiece(Dossier dossier, Piece piece) {
    if (StatutDossierWorkflow.peutPasser(dossier.getStatut(), StatutDossier.ATTENTE_PIECE)) {
      dossierService.appliquerStatut(dossier, StatutDossier.ATTENTE_PIECE,
          "Automatique : pièce " + piece.getReference() + " commandée");
    }
  }

  private Piece getPieceDuDossier(Dossier dossier, Long pieceId) {
    Piece piece = pieceRepository.findById(pieceId)
        .orElseThrow(() -> new ResourceNotFoundException("Pièce", pieceId));
    if (!piece.getDossier().getId().equals(dossier.getId())) {
      // Même réponse qu'une pièce inexistante : on ne révèle pas les pièces des autres dossiers
      throw new ResourceNotFoundException("Pièce", pieceId);
    }
    return piece;
  }

  private static boolean memeMontant(BigDecimal a, BigDecimal b) {
    return a == null ? b == null : b != null && a.compareTo(b) == 0;
  }

  private static String texteOuNull(String texte) {
    return texte == null || texte.isBlank() ? null : texte.trim();
  }
}

package pcpro.atelier.features.piece.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.piece.api.dto.ChangeEtatPieceRequest;
import pcpro.atelier.features.piece.api.dto.CreatePieceRequest;
import pcpro.atelier.features.piece.api.dto.PieceDto;
import pcpro.atelier.features.piece.api.dto.UpdatePieceRequest;
import pcpro.atelier.features.piece.app.PieceService;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/pieces")
public class PieceController {

  private final PieceService pieceService;

  public PieceController(PieceService pieceService) {
    this.pieceService = pieceService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PieceDto ajouter(@PathVariable Long dossierId, @Valid @RequestBody CreatePieceRequest request) {
    return pieceService.ajouter(dossierId, request);
  }

  @PutMapping("/{pieceId}")
  public PieceDto modifier(
      @PathVariable Long dossierId,
      @PathVariable Long pieceId,
      @Valid @RequestBody UpdatePieceRequest request
  ) {
    return pieceService.modifier(dossierId, pieceId, request);
  }

  @PatchMapping("/{pieceId}/etat")
  public PieceDto changerEtat(
      @PathVariable Long dossierId,
      @PathVariable Long pieceId,
      @Valid @RequestBody ChangeEtatPieceRequest request
  ) {
    return pieceService.changerEtat(dossierId, pieceId, request);
  }
}

package pcpro.atelier.features.dossier.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.dossier.api.dto.ChangeBlocageRequest;
import pcpro.atelier.features.dossier.api.dto.ChangePieceStatutRequest;
import pcpro.atelier.features.dossier.api.dto.ChangePrioriteRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeStatutRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeTechnicienRequest;
import pcpro.atelier.features.dossier.api.dto.CreateDossierRequest;
import pcpro.atelier.features.dossier.api.dto.CreateNoteRequest;
import pcpro.atelier.features.dossier.api.dto.CreatePieceRequest;
import pcpro.atelier.features.dossier.api.dto.CreatePhotoRequest;
import pcpro.atelier.features.dossier.api.dto.DossierDetailDto;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.dossier.api.dto.NoteDto;
import pcpro.atelier.features.dossier.api.dto.PhotoDto;
import pcpro.atelier.features.dossier.api.dto.PieceDto;
import pcpro.atelier.features.dossier.app.DossierService;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

  private final DossierService dossierService;

  public DossierController(DossierService dossierService) {
    this.dossierService = dossierService;
  }

  @PostMapping
  public ResponseEntity<DossierSummaryDto> creerDossier(
      @Valid @RequestBody CreateDossierRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    DossierSummaryDto dossier = dossierService.creerDossier(request, employeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dossier);
  }

  @GetMapping
  public ResponseEntity<Page<DossierSummaryDto>> rechercherDossiers(
      @RequestParam(required = false) StatutDossier statut,
      @RequestParam(required = false) String reference,
      @RequestParam(required = false) String clientTelephone,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
  ) {
    Page<DossierSummaryDto> dossiers = dossierService.rechercherDossiers(
        statut,
        reference,
        clientTelephone,
        pageable
    );
    return ResponseEntity.ok(dossiers);
  }

  @GetMapping("/{id}")
  public ResponseEntity<DossierDetailDto> trouverDossierParId(@PathVariable Long id) {
    DossierDetailDto dossier = dossierService.trouverDossierParId(id);
    return ResponseEntity.ok(dossier);
  }

  @PatchMapping("/{id}/statut")
  public ResponseEntity<DossierSummaryDto> changerStatut(
      @PathVariable Long id,
      @Valid @RequestBody ChangeStatutRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    DossierSummaryDto dossier = dossierService.changerStatut(id, request, employeId);
    return ResponseEntity.ok(dossier);
  }

  @PostMapping("/{id}/notes")
  public ResponseEntity<NoteDto> ajouterNote(
      @PathVariable Long id,
      @Valid @RequestBody CreateNoteRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    NoteDto note = dossierService.ajouterNote(id, request, employeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(note);
  }

  @PostMapping("/{id}/pieces")
  public ResponseEntity<PieceDto> ajouterPiece(
      @PathVariable Long id,
      @Valid @RequestBody CreatePieceRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    PieceDto piece = dossierService.ajouterPiece(id, request, employeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(piece);
  }

  @PatchMapping("/{id}/pieces/{pieceId}/statut")
  public ResponseEntity<PieceDto> changerStatutPiece(
      @PathVariable Long id,
      @PathVariable Long pieceId,
      @Valid @RequestBody ChangePieceStatutRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    PieceDto piece = dossierService.changerStatutPiece(id, pieceId, request, employeId);
    return ResponseEntity.ok(piece);
  }

  @PostMapping("/{id}/photos")
  public ResponseEntity<PhotoDto> ajouterPhoto(
      @PathVariable Long id,
      @Valid @RequestBody CreatePhotoRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    PhotoDto photo = dossierService.ajouterPhoto(id, request, employeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(photo);
  }

  @PostMapping("/{id}/photos/upload")
  public ResponseEntity<PhotoDto> uploadPhoto(
      @PathVariable Long id,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "description", required = false) String description,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    PhotoDto photo = dossierService.uploadPhoto(id, file, description, employeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(photo);
  }

  @PatchMapping("/{id}/technicien")
  public ResponseEntity<DossierSummaryDto> changerTechnicien(
      @PathVariable Long id,
      @Valid @RequestBody ChangeTechnicienRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    DossierSummaryDto dossier = dossierService.changerTechnicien(id, request, employeId);
    return ResponseEntity.ok(dossier);
  }

  @PatchMapping("/{id}/priorite")
  public ResponseEntity<DossierSummaryDto> changerPriorite(
      @PathVariable Long id,
      @Valid @RequestBody ChangePrioriteRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    DossierSummaryDto dossier = dossierService.changerPriorite(id, request, employeId);
    return ResponseEntity.ok(dossier);
  }

  @PatchMapping("/{id}/blocage")
  public ResponseEntity<DossierSummaryDto> changerBlocage(
      @PathVariable Long id,
      @Valid @RequestBody ChangeBlocageRequest request,
      @RequestHeader(value = "X-Employe-Id", required = true) Long employeId
  ) {
    DossierSummaryDto dossier = dossierService.changerBlocage(id, request, employeId);
    return ResponseEntity.ok(dossier);
  }
}

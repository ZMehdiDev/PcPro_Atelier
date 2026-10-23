package pcpro.atelier.features.paiement.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.paiement.api.dto.CaisseDto;
import pcpro.atelier.features.paiement.api.dto.ChiffreAffairesDto;
import pcpro.atelier.features.paiement.api.dto.PaiementDto;
import pcpro.atelier.features.paiement.api.dto.PaiementRequest;
import pcpro.atelier.features.paiement.app.ChiffreAffairesService;
import pcpro.atelier.features.paiement.app.PaiementService;

@RestController
public class PaiementController {

  private final PaiementService paiementService;
  private final ChiffreAffairesService chiffreAffairesService;

  public PaiementController(PaiementService paiementService, ChiffreAffairesService chiffreAffairesService) {
    this.paiementService = paiementService;
    this.chiffreAffairesService = chiffreAffairesService;
  }

  /** Acompte ou paiement en dehors de la clôture (sinon : avec PATCH /statut vers CLOTURE). */
  @PostMapping("/api/dossiers/{dossierId}/paiements")
  @ResponseStatus(HttpStatus.CREATED)
  public PaiementDto encaisser(@PathVariable Long dossierId, @Valid @RequestBody PaiementRequest request) {
    return paiementService.encaisser(dossierId, request);
  }

  @DeleteMapping("/api/dossiers/{dossierId}/paiements/{paiementId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void annuler(@PathVariable Long dossierId, @PathVariable Long paiementId) {
    paiementService.annuler(dossierId, paiementId);
  }

  /** Caisse : encaissements d'un jour ou d'une période (du/au au format AAAA-MM-JJ, aujourd'hui par défaut). */
  @GetMapping("/api/caisse")
  @PreAuthorize("hasRole('ADMIN')")
  public CaisseDto caisse(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
  ) {
    return paiementService.caisse(du, au);
  }

  /** Chiffre d'affaires des réparations rendues sur la période : encaissé, coût des pièces, marge. */
  @GetMapping("/api/chiffre-affaires")
  @PreAuthorize("hasRole('ADMIN')")
  public ChiffreAffairesDto chiffreAffaires(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
  ) {
    return chiffreAffairesService.calculer(du, au);
  }

  /** Même chose en CSV, à ouvrir dans Excel. */
  @GetMapping("/api/chiffre-affaires/export")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> exporterChiffreAffaires(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
  ) {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(chiffreAffairesService.nomFichierCsv(du, au)).build().toString())
        .body(chiffreAffairesService.exporterCsv(du, au));
  }
}

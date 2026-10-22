package pcpro.atelier.features.paiement.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.paiement.api.dto.CaisseDto;
import pcpro.atelier.features.paiement.api.dto.PaiementDto;
import pcpro.atelier.features.paiement.api.dto.PaiementRequest;
import pcpro.atelier.features.paiement.app.PaiementService;

@RestController
public class PaiementController {

  private final PaiementService paiementService;

  public PaiementController(PaiementService paiementService) {
    this.paiementService = paiementService;
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
}

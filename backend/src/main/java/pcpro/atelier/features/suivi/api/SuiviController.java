package pcpro.atelier.features.suivi.api;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pcpro.atelier.features.suivi.api.dto.ReponseDevisRequest;
import pcpro.atelier.features.suivi.api.dto.SuiviDto;
import pcpro.atelier.features.suivi.app.SuiviService;

/**
 * Suivi de réparation pour les clients (sans connexion).
 */
@RestController
@RequestMapping("/api/public/suivi/{reference}")
public class SuiviController {

  private final SuiviService suiviService;

  public SuiviController(SuiviService suiviService) {
    this.suiviService = suiviService;
  }

  /** Avec code (lien ou QR code) ou telephone (saisi par le client). */
  @GetMapping
  public SuiviDto consulter(
      @PathVariable String reference,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String telephone,
      HttpServletRequest http
  ) {
    return suiviService.consulter(reference, code, telephone, http.getRemoteAddr());
  }

  @PostMapping("/devis")
  public SuiviDto repondreDevis(
      @PathVariable String reference,
      @Valid @RequestBody ReponseDevisRequest request,
      HttpServletRequest http
  ) {
    return suiviService.repondreDevis(reference, request.code(), request.accepte(), http.getRemoteAddr());
  }
}

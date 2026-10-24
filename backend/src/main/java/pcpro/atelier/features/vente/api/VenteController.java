package pcpro.atelier.features.vente.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.vente.api.dto.VenteDto;
import pcpro.atelier.features.vente.api.dto.VenteRequest;
import pcpro.atelier.features.vente.app.VenteService;

@RestController
@RequestMapping("/api/ventes")
public class VenteController {

  private final VenteService venteService;

  public VenteController(VenteService venteService) {
    this.venteService = venteService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VenteDto enregistrer(@Valid @RequestBody VenteRequest request) {
    return venteService.enregistrer(request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void annuler(@PathVariable Long id) {
    venteService.annuler(id);
  }
}

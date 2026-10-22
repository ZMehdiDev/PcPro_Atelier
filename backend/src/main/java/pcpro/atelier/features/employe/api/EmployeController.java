package pcpro.atelier.features.employe.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.employe.api.dto.CreateEmployeRequest;
import pcpro.atelier.features.employe.api.dto.EmployeDto;
import pcpro.atelier.features.employe.api.dto.ResetMotDePasseRequest;
import pcpro.atelier.features.employe.api.dto.UpdateEmployeRequest;
import pcpro.atelier.features.employe.app.EmployeService;

@RestController
@RequestMapping("/api/employes")
public class EmployeController {

  private final EmployeService employeService;
  private final CurrentEmploye currentEmploye;

  public EmployeController(EmployeService employeService, CurrentEmploye currentEmploye) {
    this.employeService = employeService;
    this.currentEmploye = currentEmploye;
  }

  /**
   * Liste des employés actifs (pour assigner un technicien). Les inactifs ne sont visibles que par un admin.
   */
  @GetMapping
  public List<EmployeDto> lister(@RequestParam(defaultValue = "false") boolean inclureInactifs) {
    if (inclureInactifs && !currentEmploye.estAdmin()) {
      throw new AccessDeniedException("Réservé aux administrateurs");
    }
    return employeService.lister(inclureInactifs);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public EmployeDto creer(@Valid @RequestBody CreateEmployeRequest request) {
    return employeService.creer(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public EmployeDto modifier(@PathVariable Long id, @Valid @RequestBody UpdateEmployeRequest request) {
    return employeService.modifier(id, request);
  }

  @PostMapping("/{id}/mot-de-passe")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void reinitialiserMotDePasse(@PathVariable Long id, @Valid @RequestBody ResetMotDePasseRequest request) {
    employeService.reinitialiserMotDePasse(id, request.motDePasse());
  }
}

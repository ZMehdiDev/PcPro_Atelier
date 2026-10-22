package pcpro.atelier.features.dossier.api;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.core.api.TriAutorise;
import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.dossier.api.dto.ChangePrioriteRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeStatutRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeTechnicienRequest;
import pcpro.atelier.features.dossier.api.dto.CreateDossierRequest;
import pcpro.atelier.features.dossier.api.dto.DossierDetailDto;
import pcpro.atelier.features.dossier.api.dto.DossierStatsDto;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.dossier.api.dto.UpdateDossierRequest;
import pcpro.atelier.features.dossier.app.DossierService;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

  private static final Set<String> TRIS = Set.of("createdAt", "dateEntree", "dateEstimee", "reference", "statut");

  private final DossierService dossierService;

  public DossierController(DossierService dossierService) {
    this.dossierService = dossierService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DossierSummaryDto creer(@Valid @RequestBody CreateDossierRequest request) {
    return dossierService.creer(request);
  }

  /**
   * Recherche paginée. Tous les filtres sont optionnels et cumulables.
   * q cherche dans la référence, le nom/téléphone du client, la marque, le modèle et le n° de série.
   * mesDossiers=true : seulement les dossiers assignés à l'employé connecté (remplace technicienId).
   * Tri possible (sort=) : createdAt, dateEntree, dateEstimee, reference, statut.
   */
  @GetMapping
  public Page<DossierSummaryDto> rechercher(
      @RequestParam(required = false) StatutDossier statut,
      @RequestParam(required = false) Boolean actifs,
      @RequestParam(required = false) Priorite priorite,
      @RequestParam(required = false) Long technicienId,
      @RequestParam(required = false) Boolean mesDossiers,
      @RequestParam(required = false) Boolean enRetard,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return dossierService.rechercher(
        new DossierService.Recherche(statut, actifs, priorite, technicienId, mesDossiers, enRetard, q),
        TriAutorise.verifier(pageable, TRIS)
    );
  }

  @GetMapping("/stats")
  public DossierStatsDto statistiques() {
    return dossierService.statistiques();
  }

  @GetMapping("/{id}")
  public DossierDetailDto detail(@PathVariable Long id) {
    return dossierService.detail(id);
  }

  @PutMapping("/{id}")
  public DossierSummaryDto modifier(@PathVariable Long id, @Valid @RequestBody UpdateDossierRequest request) {
    return dossierService.modifier(id, request);
  }

  @PatchMapping("/{id}/statut")
  public DossierSummaryDto changerStatut(@PathVariable Long id, @Valid @RequestBody ChangeStatutRequest request) {
    return dossierService.changerStatut(id, request.statut(), request.commentaire());
  }

  @PatchMapping("/{id}/technicien")
  public DossierSummaryDto changerTechnicien(@PathVariable Long id, @RequestBody ChangeTechnicienRequest request) {
    return dossierService.changerTechnicien(id, request.technicienId());
  }

  @PatchMapping("/{id}/priorite")
  public DossierSummaryDto changerPriorite(@PathVariable Long id, @Valid @RequestBody ChangePrioriteRequest request) {
    return dossierService.changerPriorite(id, request.priorite());
  }
}

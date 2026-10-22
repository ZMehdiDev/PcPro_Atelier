package pcpro.atelier.features.client.api;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.core.api.TriAutorise;
import pcpro.atelier.features.client.api.dto.ClientDetailDto;
import pcpro.atelier.features.client.api.dto.ClientDto;
import pcpro.atelier.features.client.api.dto.ClientRequest;
import pcpro.atelier.features.client.app.ClientService;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

  private static final Set<String> TRIS = Set.of("nom", "createdAt");

  private final ClientService clientService;

  public ClientController(ClientService clientService) {
    this.clientService = clientService;
  }

  /**
   * Recherche par nom ou téléphone (q). Sans q, liste tous les clients.
   */
  @GetMapping
  public Page<ClientDto> rechercher(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable
  ) {
    return clientService.rechercher(q, TriAutorise.verifier(pageable, TRIS));
  }

  /**
   * Fiche client avec l'historique de ses dossiers.
   */
  @GetMapping("/{id}")
  public ClientDetailDto detail(@PathVariable Long id) {
    return clientService.detail(id);
  }

  @PutMapping("/{id}")
  public ClientDto modifier(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
    return clientService.modifier(id, request);
  }
}

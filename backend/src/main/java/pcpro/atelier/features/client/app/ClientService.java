package pcpro.atelier.features.client.app;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.TelephoneNormalizer;
import pcpro.atelier.features.client.api.dto.ClientDetailDto;
import pcpro.atelier.features.client.api.dto.ClientDto;
import pcpro.atelier.features.client.api.dto.ClientRequest;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.client.infra.ClientRepository;
import pcpro.atelier.features.dossier.app.DossierMapper;
import pcpro.atelier.features.dossier.infra.DossierRepository;

@Service
@Transactional
public class ClientService {

  private final ClientRepository clientRepository;
  private final DossierRepository dossierRepository;
  private final DossierMapper dossierMapper;

  public ClientService(
      ClientRepository clientRepository,
      DossierRepository dossierRepository,
      DossierMapper dossierMapper
  ) {
    this.clientRepository = clientRepository;
    this.dossierRepository = dossierRepository;
    this.dossierMapper = dossierMapper;
  }

  @Transactional(readOnly = true)
  public Page<ClientDto> rechercher(String q, Pageable pageable) {
    return clientRepository.findAll(recherche(q), pageable).map(ClientService::toDto);
  }

  /**
   * Nom qui contient q (insensible à la casse) ou téléphone qui contient les chiffres de q.
   */
  private static Specification<Client> recherche(String q) {
    if (q == null || q.isBlank()) {
      return Specification.unrestricted();
    }
    String motif = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    String telephone = TelephoneNormalizer.normaliser(q);
    return (root, query, cb) -> telephone == null
        ? cb.like(cb.lower(root.get("nom")), motif)
        : cb.or(cb.like(cb.lower(root.get("nom")), motif), cb.like(root.get("telephone"), "%" + telephone + "%"));
  }

  @Transactional(readOnly = true)
  public ClientDetailDto detail(Long id) {
    Client client = getOrThrow(id);
    var dossiers = dossierRepository.findByClientOrderByCreatedAtDesc(client).stream()
        .map(dossierMapper::toSummaryDto)
        .toList();
    return new ClientDetailDto(toDto(client), dossiers);
  }

  public ClientDto modifier(Long id, ClientRequest request) {
    Client client = getOrThrow(id);
    String telephone = telephoneObligatoire(request.telephone());
    clientRepository.findByTelephone(telephone)
        .filter(autre -> !autre.getId().equals(id))
        .ifPresent(autre -> {
          throw new ConflictException("Ce numéro de téléphone est déjà utilisé par le client " + autre.getNom());
        });
    client.setNom(request.nom().trim());
    client.setTelephone(telephone);
    client.setEmail(emailOuNull(request.email()));
    return toDto(client);
  }

  /**
   * Retrouve le client par son téléphone (normalisé) ou le crée. Le nom et l'email sont mis à jour
   * s'ils sont fournis (le client a pu changer d'adresse email depuis sa dernière visite).
   */
  public Client trouverOuCreer(ClientRequest request) {
    String telephone = telephoneObligatoire(request.telephone());
    Client client = clientRepository.findByTelephone(telephone).orElseGet(() -> {
      Client nouveau = new Client();
      nouveau.setTelephone(telephone);
      return nouveau;
    });
    client.setNom(request.nom().trim());
    String email = emailOuNull(request.email());
    if (email != null) {
      client.setEmail(email);
    }
    return clientRepository.save(client);
  }

  public Client getOrThrow(Long id) {
    return clientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client", id));
  }

  private static String telephoneObligatoire(String telephone) {
    String normalise = TelephoneNormalizer.normaliser(telephone);
    if (normalise == null || normalise.replace("+", "").length() < 6) {
      throw new BadRequestException("Numéro de téléphone invalide");
    }
    return normalise;
  }

  private static String emailOuNull(String email) {
    return email == null || email.isBlank() ? null : email.trim();
  }

  public static ClientDto toDto(Client client) {
    return new ClientDto(
        client.getId(),
        client.getNom(),
        client.getTelephone(),
        client.getEmail(),
        client.getCreatedAt()
    );
  }
}

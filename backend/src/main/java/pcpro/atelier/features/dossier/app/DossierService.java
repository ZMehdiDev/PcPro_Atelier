package pcpro.atelier.features.dossier.app;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.enums.TypeAppareil;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.core.util.ReferenceGenerator;
import pcpro.atelier.core.util.StatutDossierWorkflow;
import pcpro.atelier.features.client.app.ClientService;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.dossier.api.dto.ChangeBlocageRequest;
import pcpro.atelier.features.dossier.api.dto.CreateDossierRequest;
import pcpro.atelier.features.dossier.api.dto.DossierDetailDto;
import pcpro.atelier.features.dossier.api.dto.DossierStatsDto;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.dossier.api.dto.UpdateDossierRequest;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.dossier.infra.DossierSpecifications;
import pcpro.atelier.features.employe.app.EmployeService;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.note.infra.NoteRepository;
import pcpro.atelier.features.photo.infra.PhotoRepository;
import pcpro.atelier.features.piece.infra.PieceRepository;

@Service
@Transactional
public class DossierService {

  /** Critères de recherche de dossiers (tous optionnels). */
  public record Recherche(
      StatutDossier statut,
      Boolean actifs,
      Priorite priorite,
      Boolean bloque,
      Long technicienId,
      Boolean enRetard,
      String q
  ) {}

  private final DossierRepository dossierRepository;
  private final ClientService clientService;
  private final EmployeService employeService;
  private final EvenementService evenementService;
  private final ReferenceGenerator referenceGenerator;
  private final PieceRepository pieceRepository;
  private final NoteRepository noteRepository;
  private final PhotoRepository photoRepository;
  private final DossierMapper mapper;
  private final CurrentEmploye currentEmploye;
  private final Clock clock;

  public DossierService(
      DossierRepository dossierRepository,
      ClientService clientService,
      EmployeService employeService,
      EvenementService evenementService,
      ReferenceGenerator referenceGenerator,
      PieceRepository pieceRepository,
      NoteRepository noteRepository,
      PhotoRepository photoRepository,
      DossierMapper mapper,
      CurrentEmploye currentEmploye,
      Clock clock
  ) {
    this.dossierRepository = dossierRepository;
    this.clientService = clientService;
    this.employeService = employeService;
    this.evenementService = evenementService;
    this.referenceGenerator = referenceGenerator;
    this.pieceRepository = pieceRepository;
    this.noteRepository = noteRepository;
    this.photoRepository = photoRepository;
    this.mapper = mapper;
    this.currentEmploye = currentEmploye;
    this.clock = clock;
  }

  // ---------- Lecture ----------

  @Transactional(readOnly = true)
  public Page<DossierSummaryDto> rechercher(Recherche recherche, Pageable pageable) {
    Specification<Dossier> spec = Stream.of(
        DossierSpecifications.statut(recherche.statut()),
        DossierSpecifications.actifs(recherche.actifs()),
        DossierSpecifications.priorite(recherche.priorite()),
        DossierSpecifications.bloque(recherche.bloque()),
        DossierSpecifications.technicien(recherche.technicienId()),
        DossierSpecifications.enRetard(recherche.enRetard(), LocalDate.now(clock)),
        DossierSpecifications.recherche(recherche.q())
    ).filter(Objects::nonNull).reduce(Specification.unrestricted(), Specification::and);
    return dossierRepository.findAll(spec, pageable).map(mapper::toSummaryDto);
  }

  @Transactional(readOnly = true)
  public DossierDetailDto detail(Long id) {
    Dossier dossier = getOrThrow(id);
    return mapper.toDetailDto(
        dossier,
        pieceRepository.findByDossierOrderByCreatedAtAscIdAsc(dossier),
        noteRepository.findByDossierWithAuteur(dossier),
        photoRepository.findByDossierOrderByCreatedAtAscIdAsc(dossier),
        evenementService.findByDossier(dossier)
    );
  }

  @Transactional(readOnly = true)
  public DossierStatsDto statistiques() {
    Map<StatutDossier, Long> parStatut = new EnumMap<>(StatutDossier.class);
    for (StatutDossier statut : StatutDossier.values()) {
      parStatut.put(statut, 0L);
    }
    dossierRepository.compterParStatut().forEach(c -> parStatut.put(c.getStatut(), c.getTotal()));

    long actifs = parStatut.entrySet().stream()
        .filter(e -> e.getKey() != StatutDossier.CLOTURE)
        .mapToLong(Map.Entry::getValue)
        .sum();
    return new DossierStatsDto(
        parStatut,
        actifs,
        dossierRepository.compterBloques(),
        dossierRepository.compterEnRetard(LocalDate.now(clock))
    );
  }

  // ---------- Création / modification ----------

  public DossierSummaryDto creer(CreateDossierRequest request) {
    Client client = resoudreClient(request);

    Dossier dossier = new Dossier();
    dossier.setReference(referenceGenerator.generateReference());
    dossier.setClient(client);
    appliquerAppareil(dossier, request.appareil().type(), request.appareil().marque(),
        request.appareil().modele(), request.appareil().numeroSerie());
    dossier.setDescriptionProbleme(texteOuNull(request.descriptionProbleme()));
    dossier.setAccessoires(texteOuNull(request.accessoires()));
    dossier.setStatut(StatutDossier.RECU);
    dossier.setPriorite(request.priorite() != null ? request.priorite() : Priorite.NORMALE);
    dossier.setDateEntree(LocalDate.now(clock));
    dossier.setDateEstimee(validerDateEstimee(request.dateEstimee(), dossier.getDateEntree()));
    if (request.technicienAssigneId() != null) {
      dossier.setTechnicienAssigne(employeService.findByIdAndActifOrThrow(request.technicienAssigneId()));
    }
    dossier = dossierRepository.save(dossier);

    evenementService.journaliser(
        dossier,
        TypeActionEvenement.CREATION_DOSSIER,
        null,
        "Statut: " + dossier.getStatut(),
        dossier.getDescriptionProbleme()
    );
    return mapper.toSummaryDto(dossier);
  }

  public DossierSummaryDto modifier(Long id, UpdateDossierRequest request) {
    Dossier dossier = getModifiableOrThrow(id);

    List<String> changements = new ArrayList<>();
    comparer(changements, "Type", dossier.getTypeAppareil(), request.appareil().type());
    comparer(changements, "Marque", dossier.getMarque(), texteOuNull(request.appareil().marque()));
    comparer(changements, "Modèle", dossier.getModele(), texteOuNull(request.appareil().modele()));
    comparer(changements, "N° série", dossier.getNumeroSerie(), texteOuNull(request.appareil().numeroSerie()));
    comparer(changements, "Description", dossier.getDescriptionProbleme(), texteOuNull(request.descriptionProbleme()));
    comparer(changements, "Accessoires", dossier.getAccessoires(), texteOuNull(request.accessoires()));
    comparer(changements, "Date estimée", dossier.getDateEstimee(), request.dateEstimee());
    if (changements.isEmpty()) {
      return mapper.toSummaryDto(dossier);
    }

    appliquerAppareil(dossier, request.appareil().type(), request.appareil().marque(),
        request.appareil().modele(), request.appareil().numeroSerie());
    dossier.setDescriptionProbleme(texteOuNull(request.descriptionProbleme()));
    dossier.setAccessoires(texteOuNull(request.accessoires()));
    dossier.setDateEstimee(validerDateEstimee(request.dateEstimee(), dossier.getDateEntree()));

    evenementService.journaliser(dossier, TypeActionEvenement.MODIF_DOSSIER, null,
        String.join(", ", changements), null);
    return mapper.toSummaryDto(dossier);
  }

  public DossierSummaryDto changerStatut(Long id, StatutDossier nouveauStatut, String commentaire) {
    Dossier dossier = getOrThrow(id);
    StatutDossier ancienStatut = dossier.getStatut();

    StatutDossierWorkflow.validerTransition(ancienStatut, nouveauStatut);
    if (ancienStatut == StatutDossier.CLOTURE && !currentEmploye.estAdmin()) {
      throw new AccessDeniedException("Seul un administrateur peut rouvrir un dossier clôturé");
    }
    if (nouveauStatut == StatutDossier.CLOTURE && dossier.isBloque()) {
      throw new ConflictException("Impossible de clôturer un dossier bloqué (" + dossier.getRaisonBlocage() + ")");
    }

    appliquerStatut(dossier, nouveauStatut, commentaire);
    return mapper.toSummaryDto(dossier);
  }

  /**
   * Change le statut sans contrôle de droits (utilisé pour les changements automatiques, ex : pièce commandée).
   * La transition doit avoir été vérifiée par l'appelant.
   */
  public void appliquerStatut(Dossier dossier, StatutDossier nouveauStatut, String commentaire) {
    StatutDossier ancienStatut = dossier.getStatut();
    dossier.setStatut(nouveauStatut);

    TypeActionEvenement type = TypeActionEvenement.CHANGEMENT_STATUT;
    if (nouveauStatut == StatutDossier.CLOTURE) {
      dossier.setClosedAt(OffsetDateTime.now(clock));
      type = TypeActionEvenement.CLOTURE;
    } else if (ancienStatut == StatutDossier.CLOTURE) {
      dossier.setClosedAt(null);
      type = TypeActionEvenement.REOUVERTURE;
    }

    evenementService.journaliser(dossier, type, "Statut: " + ancienStatut, "Statut: " + nouveauStatut,
        texteOuNull(commentaire));
  }

  public DossierSummaryDto changerTechnicien(Long id, Long technicienId) {
    Dossier dossier = getModifiableOrThrow(id);
    Employe ancien = dossier.getTechnicienAssigne();
    Employe nouveau = technicienId != null ? employeService.findByIdAndActifOrThrow(technicienId) : null;

    if (Objects.equals(ancien != null ? ancien.getId() : null, technicienId)) {
      return mapper.toSummaryDto(dossier);
    }

    dossier.setTechnicienAssigne(nouveau);
    evenementService.journaliser(
        dossier,
        TypeActionEvenement.CHANGEMENT_TECHNICIEN,
        "Technicien: " + (ancien != null ? ancien.getPrenom() : "Aucun"),
        "Technicien: " + (nouveau != null ? nouveau.getPrenom() : "Aucun"),
        null
    );
    return mapper.toSummaryDto(dossier);
  }

  public DossierSummaryDto changerPriorite(Long id, Priorite priorite) {
    Dossier dossier = getModifiableOrThrow(id);
    Priorite ancienne = dossier.getPriorite();
    if (ancienne == priorite) {
      return mapper.toSummaryDto(dossier);
    }

    dossier.setPriorite(priorite);
    evenementService.journaliser(dossier, TypeActionEvenement.CHANGEMENT_PRIORITE,
        "Priorité: " + ancienne, "Priorité: " + priorite, null);
    return mapper.toSummaryDto(dossier);
  }

  public DossierSummaryDto changerBlocage(Long id, ChangeBlocageRequest request) {
    Dossier dossier = getModifiableOrThrow(id);
    boolean bloque = request.bloque();
    String raison = texteOuNull(request.raisonBlocage());

    if (bloque && raison == null) {
      throw new BadRequestException("La raison de blocage est obligatoire lors du blocage d'un dossier");
    }
    if (dossier.isBloque() == bloque && Objects.equals(dossier.getRaisonBlocage(), bloque ? raison : null)) {
      return mapper.toSummaryDto(dossier);
    }

    boolean ancienBlocage = dossier.isBloque();
    dossier.setBloque(bloque);
    dossier.setRaisonBlocage(bloque ? raison : null);
    evenementService.journaliser(dossier, TypeActionEvenement.CHANGEMENT_BLOCAGE,
        "Bloqué: " + ancienBlocage, "Bloqué: " + bloque, raison);
    return mapper.toSummaryDto(dossier);
  }

  // ---------- Utilitaires partagés avec pièces / photos / notes ----------

  @Transactional(readOnly = true)
  public Dossier getOrThrow(Long id) {
    return dossierRepository.findByIdWithRelations(id)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", id));
  }

  /**
   * Dossier non clôturé (un dossier clôturé n'accepte plus que des notes).
   */
  public Dossier getModifiableOrThrow(Long id) {
    Dossier dossier = getOrThrow(id);
    if (dossier.estCloture()) {
      throw new ConflictException("Le dossier " + dossier.getReference() + " est clôturé : rouvrez-le pour le modifier");
    }
    return dossier;
  }

  // ---------- Privé ----------

  private Client resoudreClient(CreateDossierRequest request) {
    if ((request.clientId() == null) == (request.client() == null)) {
      throw new BadRequestException("Indiquez soit clientId (client existant), soit client (nouveau client)");
    }
    return request.clientId() != null
        ? clientService.getOrThrow(request.clientId())
        : clientService.trouverOuCreer(request.client());
  }

  private static void appliquerAppareil(Dossier dossier, TypeAppareil type,
      String marque, String modele, String numeroSerie) {
    dossier.setTypeAppareil(type);
    dossier.setMarque(texteOuNull(marque));
    dossier.setModele(texteOuNull(modele));
    dossier.setNumeroSerie(texteOuNull(numeroSerie));
  }

  private static LocalDate validerDateEstimee(LocalDate dateEstimee, LocalDate dateEntree) {
    if (dateEstimee != null && dateEstimee.isBefore(dateEntree)) {
      throw new BadRequestException("La date estimée ne peut pas être antérieure à la date d'entrée");
    }
    return dateEstimee;
  }

  private static void comparer(List<String> changements, String libelle, Object avant, Object apres) {
    if (!Objects.equals(avant, apres)) {
      changements.add(libelle);
    }
  }

  static String texteOuNull(String texte) {
    return texte == null || texte.isBlank() ? null : texte.trim();
  }
}

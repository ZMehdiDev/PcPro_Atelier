package pcpro.atelier.features.dossier.app;

import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.features.dossier.api.dto.CreatePhotoRequest;
import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.PieceStateTransitionValidator;
import pcpro.atelier.core.util.ReferenceGenerator;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.client.infra.ClientRepository;
import pcpro.atelier.features.dossier.api.dto.ChangeBlocageRequest;
import pcpro.atelier.features.dossier.api.dto.ChangePieceStatutRequest;
import pcpro.atelier.features.dossier.api.dto.ChangePrioriteRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeStatutRequest;
import pcpro.atelier.features.dossier.api.dto.ChangeTechnicienRequest;
import pcpro.atelier.features.dossier.api.dto.ClientDto;
import pcpro.atelier.features.dossier.api.dto.ClientInfoDto;
import pcpro.atelier.features.dossier.api.dto.CreateDossierRequest;
import pcpro.atelier.features.dossier.api.dto.CreateNoteRequest;
import pcpro.atelier.features.dossier.api.dto.CreatePieceRequest;
import pcpro.atelier.features.dossier.api.dto.DossierDetailDto;
import pcpro.atelier.features.dossier.api.dto.DossierSummaryDto;
import pcpro.atelier.features.dossier.api.dto.EvenementDto;
import pcpro.atelier.features.dossier.api.dto.NoteDto;
import pcpro.atelier.features.dossier.api.dto.PhotoDto;
import pcpro.atelier.features.dossier.api.dto.PieceDto;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.employe.app.EmployeService;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.note.domain.Note;
import pcpro.atelier.features.note.infra.NoteRepository;
import pcpro.atelier.features.photo.domain.Photo;
import pcpro.atelier.features.photo.infra.PhotoRepository;
import pcpro.atelier.core.service.FileStorageService;
import pcpro.atelier.features.piece.domain.Piece;
import pcpro.atelier.features.piece.infra.PieceRepository;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DossierService {

  private final DossierRepository dossierRepository;
  private final ClientRepository clientRepository;
  private final EmployeService employeService;
  private final EvenementService evenementService;
  private final ReferenceGenerator referenceGenerator;
  private final PieceRepository pieceRepository;
  private final NoteRepository noteRepository;
  private final PhotoRepository photoRepository;
  private final FileStorageService fileStorageService;

  public DossierService(
      DossierRepository dossierRepository,
      ClientRepository clientRepository,
      EmployeService employeService,
      EvenementService evenementService,
      ReferenceGenerator referenceGenerator,
      PieceRepository pieceRepository,
      NoteRepository noteRepository,
      PhotoRepository photoRepository,
      FileStorageService fileStorageService
  ) {
    this.dossierRepository = dossierRepository;
    this.clientRepository = clientRepository;
    this.employeService = employeService;
    this.evenementService = evenementService;
    this.referenceGenerator = referenceGenerator;
    this.pieceRepository = pieceRepository;
    this.noteRepository = noteRepository;
    this.photoRepository = photoRepository;
    this.fileStorageService = fileStorageService;
  }

  public DossierSummaryDto creerDossier(CreateDossierRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Upsert client
    Client client = trouverOuCreerClient(request.client());
    
    // Créer le dossier avec retry en cas de collision de référence
    Dossier dossier = null;
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        // Générer une référence unique
        String reference = referenceGenerator.generateReference();
        
        // Créer le dossier
        dossier = new Dossier();
        dossier.setReference(reference);
        dossier.setClient(client);
        dossier.setTypeAppareil(request.appareil().type());
        dossier.setMarque(request.appareil().marque());
        dossier.setModele(request.appareil().modele());
        dossier.setNumeroSerie(request.appareil().numeroSerie());
        dossier.setStatut(StatutDossier.RECU);
        dossier.setPriorite(request.priorite() != null ? request.priorite() : Priorite.NORMALE);
        dossier.setDateEntree(LocalDate.now());
        
        if (request.technicienAssigneId() != null) {
          Employe technicien = employeService.findByIdAndActifOrThrow(request.technicienAssigneId());
          dossier.setTechnicienAssigne(technicien);
        }
        
        dossier = dossierRepository.save(dossier);
        break; // Succès, sortir de la boucle
      } catch (DataIntegrityViolationException e) {
        // Collision de référence détectée, réessayer avec une nouvelle référence
        if (attempt == 2) {
          // Dernière tentative échouée
          throw new ConflictException("Impossible de créer le dossier : collision de référence après plusieurs tentatives");
        }
        // Continuer la boucle pour réessayer
      }
    }
    
    if (dossier == null) {
      throw new ConflictException("Impossible de créer le dossier : échec après plusieurs tentatives");
    }
    
    // Créer l'événement d'audit
    String commentaire = request.descriptionProbleme() != null ? request.descriptionProbleme() : null;
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.CREATION_DOSSIER,
        null,
        "Statut: " + dossier.getStatut(),
        commentaire
    );
    
    return toSummaryDto(dossier);
  }

  @Transactional(readOnly = true)
  public Page<DossierSummaryDto> rechercherDossiers(
      StatutDossier statut,
      String reference,
      String clientTelephone,
      Pageable pageable
  ) {
    Page<Dossier> dossiers = dossierRepository.search(statut, reference, clientTelephone, pageable);
    return dossiers.map(this::toSummaryDto);
  }

  @Transactional(readOnly = true)
  public DossierDetailDto trouverDossierParId(Long id) {
    Dossier dossier = dossierRepository.findByIdWithRelations(id)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", id));
    
    return toDetailDto(dossier);
  }

  public DossierSummaryDto changerStatut(Long dossierId, ChangeStatutRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier avec relations (client fetché pour éviter lazy)
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Vérifier que le statut change vraiment
    StatutDossier ancienStatut = dossier.getStatut();
    if (ancienStatut == request.statut()) {
      return toSummaryDto(dossier); // Pas de changement
    }
    
    // Changer le statut
    dossier.setStatut(request.statut());
    
    // Si CLOTURE, mettre closedAt
    if (request.statut() == StatutDossier.CLOTURE) {
      dossier.setClosedAt(java.time.OffsetDateTime.now());
    }
    
    dossier = dossierRepository.save(dossier);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.CHANGEMENT_STATUT,
        "Statut: " + ancienStatut,
        "Statut: " + request.statut(),
        request.commentaire()
    );
    
    return toSummaryDto(dossier);
  }

  public NoteDto ajouterNote(Long dossierId, CreateNoteRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Créer la note
    Note note = new Note();
    note.setDossier(dossier);
    note.setContenu(request.contenu());
    note.setSensible(request.sensible());
    note = noteRepository.save(note);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.AJOUT_NOTE,
        null,
        "Note ajoutée (ID: " + note.getId() + ")",
        request.sensible() ? "Note sensible" : null
    );
    
    return new NoteDto(note.getId(), note.getContenu(), note.isSensible(), note.getCreatedAt());
  }

  public PieceDto ajouterPiece(Long dossierId, CreatePieceRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Créer la pièce
    Piece piece = new Piece();
    piece.setDossier(dossier);
    piece.setReference(request.reference());
    piece.setFournisseur(request.fournisseur());
    piece.setCout(request.cout());
    piece.setEtat(request.etat());
    piece = pieceRepository.save(piece);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.AJOUT_PIECE,
        null,
        "Pièce ajoutée: " + request.reference() + " (" + request.etat() + ")",
        null
    );
    
    return new PieceDto(
        piece.getId(),
        piece.getReference(),
        piece.getFournisseur(),
        piece.getCout(),
        piece.getEtat(),
        piece.getCreatedAt()
    );
  }

  public PieceDto changerStatutPiece(Long dossierId, Long pieceId, ChangePieceStatutRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Trouver la pièce
    Piece piece = pieceRepository.findById(pieceId)
        .orElseThrow(() -> new ResourceNotFoundException("Pièce", pieceId));
    
    // Vérifier que la pièce appartient au dossier
    if (!piece.getDossier().getId().equals(dossierId)) {
      throw new IllegalArgumentException(
          "La pièce " + pieceId + " n'appartient pas au dossier " + dossierId
      );
    }
    
    // Valider la transition d'état
    EtatPiece ancienEtat = piece.getEtat();
    EtatPiece nouvelEtat = request.nouvelEtat();
    PieceStateTransitionValidator.validerTransition(ancienEtat, nouvelEtat);
    
    // Mettre à jour l'état de la pièce
    piece.setEtat(nouvelEtat);
    piece = pieceRepository.save(piece);
    
    // Logique optionnelle : mettre le dossier en ATTENTE_PIECE si nécessaire
    // Si une pièce passe en COMMANDEE ou RECUE, et que le dossier n'est pas déjà en ATTENTE_PIECE,
    // on peut automatiquement le mettre en ATTENTE_PIECE (sauf s'il est déjà en CLOTURE)
    StatutDossier ancienStatutDossier = dossier.getStatut();
    if ((nouvelEtat == EtatPiece.COMMANDEE || nouvelEtat == EtatPiece.RECUE) 
        && ancienStatutDossier != StatutDossier.ATTENTE_PIECE
        && ancienStatutDossier != StatutDossier.CLOTURE) {
      dossier.setStatut(StatutDossier.ATTENTE_PIECE);
      dossierRepository.save(dossier);
      
      // Créer un événement pour le changement de statut du dossier
      evenementService.creerEvenement(
          dossier,
          employe,
          TypeActionEvenement.CHANGEMENT_STATUT,
          ancienStatutDossier.toString(),
          StatutDossier.ATTENTE_PIECE.toString(),
          "Statut automatiquement mis à jour suite au changement d'état de la pièce " + piece.getReference(),
          null
      );
    }
    
    // Créer l'événement d'audit pour le changement de statut de la pièce
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.CHANGEMENT_STATUT_PIECE,
        ancienEtat.toString(),
        nouvelEtat.toString(),
        request.commentaire() != null ? request.commentaire() : 
            "Changement d'état de la pièce " + piece.getReference() + " : " + ancienEtat + " -> " + nouvelEtat,
        null
    );
    
    return new PieceDto(
        piece.getId(),
        piece.getReference(),
        piece.getFournisseur(),
        piece.getCout(),
        piece.getEtat(),
        piece.getCreatedAt()
    );
  }

  public PhotoDto ajouterPhoto(Long dossierId, CreatePhotoRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Créer la photo (méthode legacy pour compatibilité, utilise uploadPhoto pour upload réel)
    Photo photo = new Photo();
    photo.setDossier(dossier);
    photo.setCheminFichier(request.cheminFichier());
    photo.setMimeType("image/jpeg"); // Valeur par défaut pour compatibilité
    photo.setTailleBytes(0L); // Valeur par défaut pour compatibilité
    photo.setDescription(request.description());
    photo = photoRepository.save(photo);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.AJOUT_PHOTO,
        null,
        "Photo ajoutée: " + request.cheminFichier(),
        request.description()
    );
    
    return new PhotoDto(
        photo.getId(), 
        photo.getCheminFichier(), 
        photo.getMimeType(),
        photo.getTailleBytes(),
        photo.getDescription(), 
        photo.getCreatedAt()
    );
  }

  public PhotoDto uploadPhoto(Long dossierId, MultipartFile file, String description, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Vérifier que le fichier n'est pas vide
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Le fichier est vide");
    }
    
    try {
      // Lire les bytes du fichier
      byte[] fileBytes = file.getBytes();
      String mimeType = file.getContentType();
      String originalFilename = file.getOriginalFilename();
      
      // Stocker le fichier sur disque
      String relativePath = fileStorageService.storeFile(fileBytes, originalFilename, mimeType, dossierId);
      
      // Créer l'entité Photo
      Photo photo = new Photo();
      photo.setDossier(dossier);
      photo.setCheminFichier(relativePath);
      photo.setMimeType(mimeType);
      photo.setTailleBytes((long) fileBytes.length);
      photo.setDescription(description);
      photo = photoRepository.save(photo);
      
      // Créer l'événement d'audit
      evenementService.creerEvenement(
          dossier,
          employe,
          TypeActionEvenement.AJOUT_PHOTO,
          null,
          "Photo uploadée: " + originalFilename + " (" + (fileBytes.length / 1024) + " KB)",
          description
      );
      
      return new PhotoDto(
          photo.getId(), 
          photo.getCheminFichier(), 
          photo.getMimeType(),
          photo.getTailleBytes(),
          photo.getDescription(), 
          photo.getCreatedAt()
      );
      
    } catch (IOException e) {
      throw new RuntimeException("Erreur lors de la lecture du fichier", e);
    }
  }

  public DossierSummaryDto changerTechnicien(Long dossierId, ChangeTechnicienRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Vérifier et assigner le technicien
    Employe ancienTechnicien = dossier.getTechnicienAssigne();
    Employe nouveauTechnicien = employeService.findByIdAndActifOrThrow(request.technicienId());
    
    dossier.setTechnicienAssigne(nouveauTechnicien);
    dossier = dossierRepository.save(dossier);
    
    // Créer l'événement d'audit
    String ancienneValeur = ancienTechnicien != null ? ancienTechnicien.getPrenom() : "Aucun";
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.ACTION_SENSIBLE,
        "Technicien: " + ancienneValeur,
        "Technicien: " + nouveauTechnicien.getPrenom(),
        null
    );
    
    return toSummaryDto(dossier);
  }

  public DossierSummaryDto changerPriorite(Long dossierId, ChangePrioriteRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Vérifier que la priorité change vraiment
    Priorite anciennePriorite = dossier.getPriorite();
    if (anciennePriorite == request.priorite()) {
      return toSummaryDto(dossier); // Pas de changement
    }
    
    // Changer la priorité
    dossier.setPriorite(request.priorite());
    dossier = dossierRepository.save(dossier);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.ACTION_SENSIBLE,
        "Priorité: " + anciennePriorite,
        "Priorité: " + request.priorite(),
        null
    );
    
    return toSummaryDto(dossier);
  }

  public DossierSummaryDto changerBlocage(Long dossierId, ChangeBlocageRequest request, Long employeId) {
    // Vérifier que l'employé existe et est actif
    Employe employe = employeService.findByIdAndActifOrThrow(employeId);
    
    // Trouver le dossier
    Dossier dossier = dossierRepository.findByIdWithRelations(dossierId)
        .orElseThrow(() -> new ResourceNotFoundException("Dossier", dossierId));
    
    // Vérifier que le blocage change vraiment
    boolean ancienBlocage = dossier.isBloque();
    if (ancienBlocage == request.bloque()) {
      return toSummaryDto(dossier); // Pas de changement
    }
    
    // Validation : si on bloque, la raison doit être fournie
    if (request.bloque() && (request.raisonBlocage() == null || request.raisonBlocage().trim().isEmpty())) {
      throw new IllegalArgumentException("La raison de blocage est obligatoire lors du blocage d'un dossier");
    }
    
    // Changer le blocage
    dossier.setBloque(request.bloque());
    dossier.setRaisonBlocage(request.bloque() ? request.raisonBlocage() : null);
    dossier = dossierRepository.save(dossier);
    
    // Créer l'événement d'audit
    evenementService.creerEvenement(
        dossier,
        employe,
        TypeActionEvenement.ACTION_SENSIBLE,
        "Bloqué: " + ancienBlocage,
        "Bloqué: " + request.bloque(),
        request.bloque() ? request.raisonBlocage() : null
    );
    
    return toSummaryDto(dossier);
  }

  private Client trouverOuCreerClient(ClientDto clientDto) {
    return clientRepository.findByTelephone(clientDto.telephone())
        .map(existing -> {
          // Mettre à jour le nom si fourni et différent
          if (clientDto.nom() != null && !clientDto.nom().equals(existing.getNom())) {
            existing.setNom(clientDto.nom());
            return clientRepository.save(existing);
          }
          return existing;
        })
        .orElseGet(() -> {
          Client nouveau = new Client();
          nouveau.setNom(clientDto.nom());
          nouveau.setTelephone(clientDto.telephone());
          return clientRepository.save(nouveau);
        });
  }

  private DossierSummaryDto toSummaryDto(Dossier dossier) {
    return new DossierSummaryDto(
        dossier.getId(),
        dossier.getReference(),
        dossier.getClient().getNom(),
        dossier.getClient().getTelephone(),
        dossier.getTypeAppareil(),
        dossier.getMarque(),
        dossier.getModele(),
        dossier.getStatut(),
        dossier.getPriorite(),
        dossier.getDateEntree(),
        dossier.getCreatedAt()
    );
  }

  private DossierDetailDto toDetailDto(Dossier dossier) {
    // Charger les relations lazy
    var pieces = pieceRepository.findByDossier(dossier);
    var notes = noteRepository.findByDossier(dossier);
    var photos = photoRepository.findByDossier(dossier);
    var evenements = evenementService.findByDossier(dossier);
    
    return new DossierDetailDto(
        dossier.getId(),
        dossier.getReference(),
        new ClientInfoDto(
            dossier.getClient().getId(),
            dossier.getClient().getNom(),
            dossier.getClient().getTelephone(),
            dossier.getClient().getCreatedAt()
        ),
        dossier.getTypeAppareil(),
        dossier.getMarque(),
        dossier.getModele(),
        dossier.getNumeroSerie(),
        dossier.getStatut(),
        dossier.isBloque(),
        dossier.getRaisonBlocage(),
        dossier.getTechnicienAssigne() != null ? dossier.getTechnicienAssigne().getPrenom() : null,
        dossier.getPriorite(),
        dossier.getDateEntree(),
        dossier.getDateEstimee(),
        dossier.getCreatedAt(),
        dossier.getClosedAt(),
        pieces.stream().map(p -> new PieceDto(
            p.getId(),
            p.getReference(),
            p.getFournisseur(),
            p.getCout(),
            p.getEtat(),
            p.getCreatedAt()
        )).toList(),
        notes.stream().map(n -> new NoteDto(
            n.getId(),
            n.getContenu(),
            n.isSensible(),
            n.getCreatedAt()
        )).toList(),
        photos.stream().map(p -> new PhotoDto(
            p.getId(),
            p.getCheminFichier(),
            p.getMimeType(),
            p.getTailleBytes(),
            p.getDescription(),
            p.getCreatedAt()
        )).toList(),
        evenements.stream().map(e -> new EvenementDto(
            e.getId(),
            e.getEmploye().getPrenom(),
            e.getTypeAction(),
            e.getAncienneValeur(),
            e.getNouvelleValeur(),
            e.getCommentaire(),
            e.getCreatedAt()
        )).toList()
    );
  }
}

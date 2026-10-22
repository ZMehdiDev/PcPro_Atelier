package pcpro.atelier.features.note.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.dossier.app.DossierMapper;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.note.api.dto.CreateNoteRequest;
import pcpro.atelier.features.note.api.dto.NoteDto;
import pcpro.atelier.features.note.domain.Note;
import pcpro.atelier.features.note.infra.NoteRepository;

@Service
@Transactional
public class NoteService {

  private final DossierService dossierService;
  private final NoteRepository noteRepository;
  private final EvenementService evenementService;
  private final CurrentEmploye currentEmploye;

  public NoteService(
      DossierService dossierService,
      NoteRepository noteRepository,
      EvenementService evenementService,
      CurrentEmploye currentEmploye
  ) {
    this.dossierService = dossierService;
    this.noteRepository = noteRepository;
    this.evenementService = evenementService;
    this.currentEmploye = currentEmploye;
  }

  /**
   * Ajoute une note. Autorisé même sur un dossier clôturé (ex : retour client après restitution).
   */
  public NoteDto ajouter(Long dossierId, CreateNoteRequest request) {
    Dossier dossier = dossierService.getOrThrow(dossierId);

    Note note = new Note();
    note.setDossier(dossier);
    note.setAuteur(currentEmploye.get());
    note.setContenu(request.contenu().trim());
    note.setSensible(request.estSensible());
    note = noteRepository.save(note);

    // Le contenu n'est pas recopié dans l'historique (il peut être sensible)
    evenementService.journaliser(
        dossier,
        TypeActionEvenement.AJOUT_NOTE,
        null,
        "Note ajoutée (ID: " + note.getId() + ")",
        request.estSensible() ? "Note sensible" : null
    );
    return DossierMapper.toNoteDto(note);
  }
}

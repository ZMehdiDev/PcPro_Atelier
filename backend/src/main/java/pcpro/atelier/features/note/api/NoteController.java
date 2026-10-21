package pcpro.atelier.features.note.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pcpro.atelier.features.note.api.dto.CreateNoteRequest;
import pcpro.atelier.features.note.api.dto.NoteDto;
import pcpro.atelier.features.note.app.NoteService;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteDto ajouter(@PathVariable Long dossierId, @Valid @RequestBody CreateNoteRequest request) {
    return noteService.ajouter(dossierId, request);
  }
}

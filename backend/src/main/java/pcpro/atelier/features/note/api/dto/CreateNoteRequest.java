package pcpro.atelier.features.note.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
    @NotBlank(message = "Le contenu de la note est obligatoire")
    @Size(max = 10000, message = "Le contenu ne peut pas dépasser 10000 caractères")
    String contenu,

    /** Optionnel, false par défaut. */
    Boolean sensible
) {

  public boolean estSensible() {
    return Boolean.TRUE.equals(sensible);
  }
}

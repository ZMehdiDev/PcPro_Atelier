package pcpro.atelier.features.note.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.note.domain.Note;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
  List<Note> findByDossier(Dossier dossier);
}

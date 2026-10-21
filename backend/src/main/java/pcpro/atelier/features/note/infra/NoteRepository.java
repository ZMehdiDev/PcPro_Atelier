package pcpro.atelier.features.note.infra;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.note.domain.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

  @Query("""
    select n from Note n
    left join fetch n.auteur
    where n.dossier = :dossier
    order by n.createdAt desc, n.id desc
    """)
  List<Note> findByDossierWithAuteur(@Param("dossier") Dossier dossier);
}

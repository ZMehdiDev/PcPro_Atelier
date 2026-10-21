package pcpro.atelier.features.photo.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.photo.domain.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
  List<Photo> findByDossierOrderByCreatedAtAscIdAsc(Dossier dossier);
}

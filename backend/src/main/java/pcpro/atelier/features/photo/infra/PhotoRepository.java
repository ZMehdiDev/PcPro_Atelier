package pcpro.atelier.features.photo.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.photo.domain.Photo;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
  List<Photo> findByDossier(Dossier dossier);
}

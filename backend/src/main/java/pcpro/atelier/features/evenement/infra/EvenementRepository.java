package pcpro.atelier.features.evenement.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.domain.Evenement;

import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {
  List<Evenement> findByDossierOrderByCreatedAtDesc(Dossier dossier);
}

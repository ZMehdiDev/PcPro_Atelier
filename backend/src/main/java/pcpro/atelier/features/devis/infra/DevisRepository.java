package pcpro.atelier.features.devis.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.dossier.domain.Dossier;

@Repository
public interface DevisRepository extends JpaRepository<Devis, Long> {

  @EntityGraph(attributePaths = "lignes")
  Optional<Devis> findByDossier(Dossier dossier);
}

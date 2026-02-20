package pcpro.atelier.features.evenement.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.domain.Evenement;

import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {
  List<Evenement> findByDossierOrderByCreatedAtDesc(Dossier dossier);
  
  @Query("""
    select e from Evenement e
    join fetch e.employe
    where e.dossier = :dossier
    order by e.createdAt desc
  """)
  List<Evenement> findByDossierWithEmploye(@Param("dossier") Dossier dossier);
}

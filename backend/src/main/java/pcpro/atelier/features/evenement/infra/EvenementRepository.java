package pcpro.atelier.features.evenement.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.domain.Evenement;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

  @Query("""
    select e from Evenement e
    left join fetch e.employe
    where e.dossier = :dossier
    order by e.createdAt desc, e.id desc
    """)
  List<Evenement> findByDossierWithEmploye(@Param("dossier") Dossier dossier);
}

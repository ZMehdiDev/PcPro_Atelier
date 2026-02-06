package pcpro.atelier.features.dossier.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.dossier.domain.Dossier;

import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
  List<Dossier> findByStatut(StatutDossier statut);
  
  @Query("""
    select d from Dossier d
    join d.client c
    where (:statut is null or d.statut = :statut)
      and (
        :q is null
        or lower(d.reference) like lower(concat('%', :q, '%'))
        or lower(c.nom) like lower(concat('%', :q, '%'))
        or c.telephone like concat('%', :q, '%')
      )
    order by d.createdAt desc
  """)
  List<Dossier> search(@Param("statut") StatutDossier statut, @Param("q") String q);
}

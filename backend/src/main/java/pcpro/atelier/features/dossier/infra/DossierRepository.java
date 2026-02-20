package pcpro.atelier.features.dossier.infra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.dossier.domain.Dossier;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
  List<Dossier> findByStatut(StatutDossier statut);
  
  Optional<Dossier> findByReference(String reference);
  
  @Query(value = """
    SELECT reference FROM dossier 
    WHERE reference LIKE :prefix || '%'
    ORDER BY reference DESC 
    LIMIT 1
  """, nativeQuery = true)
  String findMaxReferenceByPrefix(@Param("prefix") String prefix);
  
  @Query("""
    select d from Dossier d
    join d.client c
    where (:statut is null or d.statut = :statut)
      and (coalesce(:reference, '') = '' or lower(d.reference) like concat('%', lower(coalesce(:reference, '')), '%'))
      and (coalesce(:clientTelephone, '') = '' or c.telephone like concat('%', coalesce(:clientTelephone, ''), '%'))
    order by d.createdAt desc
  """)
  Page<Dossier> search(
      @Param("statut") StatutDossier statut,
      @Param("reference") String reference,
      @Param("clientTelephone") String clientTelephone,
      Pageable pageable
  );
  
  
  @Query("""
    select d from Dossier d
    join fetch d.client
    left join fetch d.technicienAssigne
    where d.id = :id
  """)
  Optional<Dossier> findByIdWithRelations(@Param("id") Long id);
}

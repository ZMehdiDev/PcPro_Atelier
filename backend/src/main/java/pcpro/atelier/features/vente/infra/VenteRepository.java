package pcpro.atelier.features.vente.infra;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.vente.domain.Vente;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {

  @Query("""
    select distinct v from Vente v
    join fetch v.employe
    left join fetch v.lignes
    where v.createdAt >= :debut and v.createdAt < :fin
    order by v.createdAt, v.id
    """)
  List<Vente> findEntre(@Param("debut") OffsetDateTime debut, @Param("fin") OffsetDateTime fin);
}

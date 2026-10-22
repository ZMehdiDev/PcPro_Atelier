package pcpro.atelier.features.paiement.infra;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.paiement.domain.Paiement;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

  @Query("select p from Paiement p join fetch p.employe where p.dossier = :dossier order by p.createdAt, p.id")
  List<Paiement> findByDossier(@Param("dossier") Dossier dossier);

  @Query("""
    select p from Paiement p
    join fetch p.employe
    join fetch p.dossier d
    join fetch d.client
    where p.createdAt >= :debut and p.createdAt < :fin
    order by p.createdAt, p.id
    """)
  List<Paiement> findEntre(@Param("debut") OffsetDateTime debut, @Param("fin") OffsetDateTime fin);
}

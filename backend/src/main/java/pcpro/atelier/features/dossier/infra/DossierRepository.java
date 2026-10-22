package pcpro.atelier.features.dossier.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.dossier.domain.Dossier;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long>, JpaSpecificationExecutor<Dossier> {

  @Override
  @EntityGraph(attributePaths = {"client", "technicienAssigne"})
  Page<Dossier> findAll(Specification<Dossier> spec, Pageable pageable);

  @EntityGraph(attributePaths = {"client", "technicienAssigne"})
  List<Dossier> findByClientOrderByCreatedAtDesc(Client client);

  @Query("""
    select d from Dossier d
    join fetch d.client
    left join fetch d.technicienAssigne
    where d.id = :id
    """)
  Optional<Dossier> findByIdWithRelations(@Param("id") Long id);

  interface StatutCount {
    StatutDossier getStatut();

    long getTotal();
  }

  @Query("select d.statut as statut, count(d) as total from Dossier d group by d.statut")
  List<StatutCount> compterParStatut();

  @Query("select count(d) from Dossier d where d.bloque = true and d.statut <> pcpro.atelier.core.enums.StatutDossier.CLOTURE")
  long compterBloques();

  @Query("""
    select count(d) from Dossier d
    where d.technicienAssigne.id = :employeId and d.statut <> pcpro.atelier.core.enums.StatutDossier.CLOTURE
    """)
  long compterActifsAssignesA(@Param("employeId") Long employeId);

  @Query("""
    select count(d) from Dossier d
    where d.dateEstimee < :aujourdhui
      and d.statut not in (pcpro.atelier.core.enums.StatutDossier.PRET, pcpro.atelier.core.enums.StatutDossier.CLOTURE)
    """)
  long compterEnRetard(@Param("aujourdhui") LocalDate aujourdhui);
}

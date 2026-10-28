package pcpro.atelier.features.facture.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.facture.domain.Facture;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

  @EntityGraph(attributePaths = {"lignes", "dossier", "vente", "factureOrigine"})
  Optional<Facture> findWithLignesById(Long id);

  /** Factures et notes de crédit d'un dossier, la plus récente d'abord. */
  @Query("select f from Facture f where f.dossier.id = :dossierId order by f.id desc")
  List<Facture> findByDossierId(@Param("dossierId") Long dossierId);

  @Query("select f from Facture f where f.vente.id = :venteId order by f.id desc")
  List<Facture> findByVenteId(@Param("venteId") Long venteId);

  /** Note de crédit qui annule cette facture, s'il y en a une. */
  Optional<Facture> findByFactureOrigineId(Long factureId);

  @Query("""
    select f from Facture f
    left join fetch f.factureOrigine
    where f.dateEmission >= :du and f.dateEmission <= :au
    order by f.dateEmission, f.id
    """)
  List<Facture> findEntre(@Param("du") LocalDate du, @Param("au") LocalDate au);

  /** Notes de crédit qui annulent les factures données. */
  @Query("select n from Facture n join fetch n.factureOrigine o where o.id in :ids")
  List<Facture> notesCreditDe(@Param("ids") List<Long> ids);
}

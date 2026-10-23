package pcpro.atelier.features.piece.infra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.piece.domain.Piece;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
  List<Piece> findByDossierOrderByCreatedAtAscIdAsc(Dossier dossier);

  boolean existsByDossierAndEtatIn(Dossier dossier, Collection<EtatPiece> etats);

  interface CoutParDossier {
    Long getDossierId();

    BigDecimal getTotal();
  }

  @Query("""
    select p.dossier.id as dossierId, coalesce(sum(p.cout), 0) as total from Piece p
    where p.dossier.id in :ids and p.etat <> pcpro.atelier.core.enums.EtatPiece.ANNULEE
    group by p.dossier.id
    """)
  List<CoutParDossier> coutParDossier(@Param("ids") Collection<Long> ids);
}

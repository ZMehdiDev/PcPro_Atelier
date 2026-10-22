package pcpro.atelier.features.piece.infra;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.piece.domain.Piece;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
  List<Piece> findByDossierOrderByCreatedAtAscIdAsc(Dossier dossier);

  boolean existsByDossierAndEtatIn(Dossier dossier, Collection<EtatPiece> etats);
}

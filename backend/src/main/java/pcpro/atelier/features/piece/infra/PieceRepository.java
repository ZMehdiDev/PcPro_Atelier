package pcpro.atelier.features.piece.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.piece.domain.Piece;

import java.util.List;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
  List<Piece> findByDossier(Dossier dossier);
}

package pcpro.atelier.features.employe.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.employe.domain.Employe;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
  List<Employe> findByActifTrueOrderByPrenomAsc();

  List<Employe> findAllByOrderByPrenomAsc();

  Optional<Employe> findByLogin(String login);

  boolean existsByLogin(String login);
}

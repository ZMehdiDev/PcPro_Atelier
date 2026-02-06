package pcpro.atelier.features.employe.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.employe.domain.Employe;

import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
  List<Employe> findByActifTrue();
}

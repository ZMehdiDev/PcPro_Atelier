package pcpro.atelier.features.auth.infra;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pcpro.atelier.features.auth.domain.SessionEmploye;
import pcpro.atelier.features.employe.domain.Employe;

@Repository
public interface SessionEmployeRepository extends JpaRepository<SessionEmploye, Long> {

  @Query("select s from SessionEmploye s join fetch s.employe where s.tokenHash = :tokenHash")
  Optional<SessionEmploye> findByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query("""
    update SessionEmploye s set s.revokedAt = :maintenant
    where s.employe = :employe and s.revokedAt is null
    """)
  int revoquerToutes(@Param("employe") Employe employe, @Param("maintenant") OffsetDateTime maintenant);

  @Modifying
  @Query("delete from SessionEmploye s where s.expiresAt < :limite or s.revokedAt < :limite")
  int supprimerAnciennes(@Param("limite") OffsetDateTime limite);
}

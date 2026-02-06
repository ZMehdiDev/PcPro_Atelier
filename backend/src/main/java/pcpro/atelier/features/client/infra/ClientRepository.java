package pcpro.atelier.features.client.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pcpro.atelier.features.client.domain.Client;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
  Optional<Client> findByTelephone(String telephone);
}

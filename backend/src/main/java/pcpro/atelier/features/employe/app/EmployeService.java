package pcpro.atelier.features.employe.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.features.employe.api.dto.EmployeDto;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.employe.infra.EmployeRepository;

@Service
@Transactional(readOnly = true)
public class EmployeService {

  private final EmployeRepository employeRepository;

  public EmployeService(EmployeRepository employeRepository) {
    this.employeRepository = employeRepository;
  }

  public List<EmployeDto> findAllActifs() {
    return employeRepository.findByActifTrue().stream()
        .map(this::toDto)
        .toList();
  }

  public Employe findByIdOrThrow(Long id) {
    return employeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
  }

  public Employe findByIdAndActifOrThrow(Long id) {
    Employe employe = findByIdOrThrow(id);
    if (!employe.isActif()) {
      throw new ResourceNotFoundException("Employé actif", id);
    }
    return employe;
  }

  private EmployeDto toDto(Employe employe) {
    return new EmployeDto(
        employe.getId(),
        employe.getPrenom(),
        employe.isActif(),
        employe.getCreatedAt()
    );
  }
}

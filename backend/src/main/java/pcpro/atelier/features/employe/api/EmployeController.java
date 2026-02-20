package pcpro.atelier.features.employe.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pcpro.atelier.features.employe.api.dto.EmployeDto;
import pcpro.atelier.features.employe.app.EmployeService;

@RestController
@RequestMapping("/api/employes")
public class EmployeController {

  private final EmployeService employeService;

  public EmployeController(EmployeService employeService) {
    this.employeService = employeService;
  }

  @GetMapping
  public ResponseEntity<List<EmployeDto>> getAllActifs() {
    List<EmployeDto> employes = employeService.findAllActifs();
    return ResponseEntity.ok(employes);
  }
}

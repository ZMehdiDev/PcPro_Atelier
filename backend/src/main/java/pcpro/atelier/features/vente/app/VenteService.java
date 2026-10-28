package pcpro.atelier.features.vente.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.vente.api.dto.VenteDto;
import pcpro.atelier.features.vente.api.dto.VenteRequest;
import pcpro.atelier.features.vente.domain.Vente;
import pcpro.atelier.features.vente.domain.VenteLigne;
import pcpro.atelier.features.vente.infra.VenteRepository;

/** Ventes au comptoir, hors réparation. Elles entrent dans la caisse et le chiffre d'affaires. */
@Service
@Transactional
public class VenteService {

  private final VenteRepository venteRepository;
  private final CurrentEmploye currentEmploye;

  public VenteService(VenteRepository venteRepository, CurrentEmploye currentEmploye) {
    this.venteRepository = venteRepository;
    this.currentEmploye = currentEmploye;
  }

  public VenteDto enregistrer(VenteRequest request) {
    Vente vente = new Vente();
    vente.setMoyen(request.moyen());
    vente.setEmploye(currentEmploye.get());
    request.lignes().forEach(l -> vente.ajouterLigne(
        new VenteLigne(l.article().trim(), l.quantite(), l.prixUnitaire(), l.coutUnitaire())));
    return VenteDto.of(venteRepository.save(vente));
  }

  /** Correction d'une erreur de saisie (réservé aux administrateurs, contrôlé par le contrôleur). */
  public void annuler(Long id) {
    Vente vente = venteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vente", id));
    venteRepository.delete(vente);
  }
}

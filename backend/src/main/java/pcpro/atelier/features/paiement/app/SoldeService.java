package pcpro.atelier.features.paiement.app;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.features.devis.infra.DevisRepository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.paiement.infra.PaiementRepository;

/** Ce que le client doit encore payer : total du devis accepté moins les paiements. */
@Service
@Transactional(readOnly = true)
public class SoldeService {

  private final DevisRepository devisRepository;
  private final PaiementRepository paiementRepository;

  public SoldeService(DevisRepository devisRepository, PaiementRepository paiementRepository) {
    this.devisRepository = devisRepository;
    this.paiementRepository = paiementRepository;
  }

  /** Null s'il n'y a pas de devis accepté (montant inconnu). */
  public BigDecimal resteAPayer(Dossier dossier) {
    return devisRepository.findByDossier(dossier)
        .filter(d -> d.getStatut() == StatutDevis.ACCEPTE)
        .map(d -> d.total().subtract(PaiementService.total(paiementRepository.findByDossier(dossier))))
        .orElse(null);
  }
}

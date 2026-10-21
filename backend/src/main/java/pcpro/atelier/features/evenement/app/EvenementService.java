package pcpro.atelier.features.evenement.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.security.CurrentEmploye;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.evenement.domain.Evenement;
import pcpro.atelier.features.evenement.infra.EvenementRepository;

/**
 * Journal d'audit des dossiers : chaque action est enregistrée avec l'employé connecté qui l'a faite.
 */
@Service
@Transactional
public class EvenementService {

  private final EvenementRepository evenementRepository;
  private final CurrentEmploye currentEmploye;

  public EvenementService(EvenementRepository evenementRepository, CurrentEmploye currentEmploye) {
    this.evenementRepository = evenementRepository;
    this.currentEmploye = currentEmploye;
  }

  public Evenement journaliser(
      Dossier dossier,
      TypeActionEvenement typeAction,
      String ancienneValeur,
      String nouvelleValeur,
      String commentaire
  ) {
    Evenement evenement = new Evenement();
    evenement.setDossier(dossier);
    evenement.setEmploye(currentEmploye.get());
    evenement.setTypeAction(typeAction);
    evenement.setAncienneValeur(ancienneValeur);
    evenement.setNouvelleValeur(nouvelleValeur);
    evenement.setCommentaire(commentaire);
    return evenementRepository.save(evenement);
  }

  @Transactional(readOnly = true)
  public List<Evenement> findByDossier(Dossier dossier) {
    return evenementRepository.findByDossierWithEmploye(dossier);
  }
}

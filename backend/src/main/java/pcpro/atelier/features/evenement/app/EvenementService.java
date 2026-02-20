package pcpro.atelier.features.evenement.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.features.evenement.domain.Evenement;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.evenement.infra.EvenementRepository;

@Service
@Transactional
public class EvenementService {

  private final EvenementRepository evenementRepository;

  public EvenementService(EvenementRepository evenementRepository) {
    this.evenementRepository = evenementRepository;
  }

  public Evenement creerEvenement(
      Dossier dossier,
      Employe employe,
      TypeActionEvenement typeAction,
      String ancienneValeur,
      String nouvelleValeur,
      String commentaire
  ) {
    Evenement evenement = new Evenement();
    evenement.setDossier(dossier);
    evenement.setEmploye(employe);
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

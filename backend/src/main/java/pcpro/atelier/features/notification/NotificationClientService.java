package pcpro.atelier.features.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.BadRequestException;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ServiceIndisponibleException;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.employe.infra.EmployeRepository;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.paiement.app.SoldeService;

/**
 * Emails aux clients : "c'est prêt" (automatique), relance et devis (à la demande).
 * Chaque envoi est noté dans l'historique du dossier.
 */
@Service
public class NotificationClientService {

  private static final Logger log = LoggerFactory.getLogger(NotificationClientService.class);

  private final EmailService emailService;
  private final EmailTemplates templates;
  private final SoldeService soldeService;
  private final DossierRepository dossierRepository;
  private final EmployeRepository employeRepository;
  private final EvenementService evenementService;

  public NotificationClientService(
      EmailService emailService,
      EmailTemplates templates,
      SoldeService soldeService,
      DossierRepository dossierRepository,
      EmployeRepository employeRepository,
      EvenementService evenementService
  ) {
    this.emailService = emailService;
    this.templates = templates;
    this.soldeService = soldeService;
    this.dossierRepository = dossierRepository;
    this.employeRepository = employeRepository;
    this.evenementService = evenementService;
  }

  /**
   * Prévient le client que son appareil est prêt, après validation du changement de statut et sans faire
   * attendre l'employé. Un échec est noté dans l'historique (l'employé peut alors appeler le client).
   */
  @Async
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void surDossierPret(DossierPretEvent event) {
    Dossier dossier = dossierRepository.findByIdWithRelations(event.dossierId()).orElse(null);
    if (dossier == null || dossier.getStatut() != StatutDossier.PRET
        || dossier.getClient().getEmail() == null || !emailService.estConfigure()) {
      return;
    }
    var employe = employeRepository.getReferenceById(event.employeId());
    try {
      emailService.envoyer(templates.pret(dossier, soldeService.resteAPayer(dossier)));
      evenementService.journaliserPar(employe, dossier, TypeActionEvenement.EMAIL_CLIENT, null,
          "Email : appareil prêt", null);
    } catch (ServiceIndisponibleException e) {
      log.warn("Client du dossier {} non prévenu : {}", dossier.getReference(), e.getMessage());
      evenementService.journaliserPar(employe, dossier, TypeActionEvenement.EMAIL_CLIENT, null,
          "Échec de l'email : appareil prêt", "Le client n'a pas pu être prévenu par email, appelez-le");
    }
  }

  /** Rappel au client dont l'appareil prêt attend à l'atelier. */
  @Transactional
  public void relancer(Dossier dossier) {
    if (dossier.getStatut() != StatutDossier.PRET) {
      throw new ConflictException("Seul un appareil prêt peut faire l'objet d'une relance");
    }
    envoyer(dossier, templates.relance(dossier), "Email : relance, appareil à récupérer");
  }

  @Transactional
  public void envoyerDevis(Dossier dossier, Devis devis) {
    envoyer(dossier, templates.devis(dossier, devis), "Email : devis envoyé");
  }

  private void envoyer(Dossier dossier, Email email, String libelle) {
    if (dossier.getClient().getEmail() == null) {
      throw new BadRequestException("Le client n'a pas d'adresse email : ajoutez-la sur sa fiche");
    }
    emailService.envoyer(email);
    evenementService.journaliser(dossier, TypeActionEvenement.EMAIL_CLIENT, null, libelle, null);
  }
}

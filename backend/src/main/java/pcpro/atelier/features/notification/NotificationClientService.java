package pcpro.atelier.features.notification;

import java.util.List;

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
import pcpro.atelier.features.bondepot.BonDepotService;
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
  private final BonDepotService bonDepotService;
  private final EmailTemplates templates;
  private final SoldeService soldeService;
  private final DossierRepository dossierRepository;
  private final EmployeRepository employeRepository;
  private final EvenementService evenementService;

  public NotificationClientService(
      EmailService emailService,
      BonDepotService bonDepotService,
      EmailTemplates templates,
      SoldeService soldeService,
      DossierRepository dossierRepository,
      EmployeRepository employeRepository,
      EvenementService evenementService
  ) {
    this.emailService = emailService;
    this.bonDepotService = bonDepotService;
    this.templates = templates;
    this.soldeService = soldeService;
    this.dossierRepository = dossierRepository;
    this.employeRepository = employeRepository;
    this.evenementService = evenementService;
  }

  /** Envoie au client son bon de dépôt (exemplaire client) dès la création du dossier, s'il a un email. */
  @Async
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void surDossierCree(DossierCreeEvent event) {
    Dossier dossier = dossierRepository.findByIdWithRelations(event.dossierId()).orElse(null);
    if (dossier == null || dossier.getClient().getEmail() == null || !emailService.estConfigure()) {
      return;
    }
    BonDepotService.BonDepot bon = bonDepotService.generer(dossier, List.of(BonDepotService.Exemplaire.CLIENT));
    envoyerEnArrierePlan(event.employeId(), dossier,
        templates.depot(dossier).avecPieceJointe(bon.nomFichier(), bon.pdf()), "bon de dépôt");
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
    envoyerEnArrierePlan(event.employeId(), dossier,
        templates.pret(dossier, soldeService.resteAPayer(dossier)), "appareil prêt");
  }

  /** Envoi sans employé qui attend la réponse : un échec est noté dans l'historique (l'employé peut appeler le client). */
  private void envoyerEnArrierePlan(Long employeId, Dossier dossier, Email email, String objet) {
    var employe = employeRepository.getReferenceById(employeId);
    try {
      emailService.envoyer(email);
      evenementService.journaliserPar(employe, dossier, TypeActionEvenement.EMAIL_CLIENT, null,
          "Email : " + objet, null);
    } catch (ServiceIndisponibleException e) {
      log.warn("Email \"{}\" du dossier {} non envoyé : {}", objet, dossier.getReference(), e.getMessage());
      evenementService.journaliserPar(employe, dossier, TypeActionEvenement.EMAIL_CLIENT, null,
          "Échec de l'email : " + objet, "Le client n'a pas reçu l'email, appelez-le si besoin");
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

package pcpro.atelier.features.devis.app;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeActionEvenement;
import pcpro.atelier.core.exception.ConflictException;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.core.util.StatutDossierWorkflow;
import pcpro.atelier.features.devis.api.dto.DevisDto;
import pcpro.atelier.features.devis.api.dto.DevisRequest;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.domain.DevisLigne;
import pcpro.atelier.features.devis.infra.DevisRepository;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.employe.domain.Employe;
import pcpro.atelier.features.evenement.app.EvenementService;
import pcpro.atelier.features.notification.NotificationClientService;

/**
 * Devis : préparé par l'atelier, accepté ou refusé par le client (au téléphone, ou lui-même en ligne).
 *
 * Faire un devis met le dossier "en attente du client" ; l'accepter relance la réparation.
 */
@Service
@Transactional
public class DevisService {

  private final DevisRepository devisRepository;
  private final DossierService dossierService;
  private final EvenementService evenementService;
  private final NotificationClientService notificationService;
  private final Clock clock;

  public DevisService(
      DevisRepository devisRepository,
      DossierService dossierService,
      EvenementService evenementService,
      NotificationClientService notificationService,
      Clock clock
  ) {
    this.devisRepository = devisRepository;
    this.dossierService = dossierService;
    this.evenementService = evenementService;
    this.notificationService = notificationService;
    this.clock = clock;
  }

  /** Crée le devis du dossier, ou le remplace s'il n'a pas encore été accepté. */
  public DevisDto enregistrer(Long dossierId, DevisRequest request) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Devis devis = devisRepository.findByDossier(dossier).orElseGet(() -> {
      Devis nouveau = new Devis();
      nouveau.setDossier(dossier);
      return nouveau;
    });
    if (devis.getStatut() == StatutDevis.ACCEPTE) {
      throw new ConflictException("Le devis a déjà été accepté par le client");
    }
    BigDecimal ancienTotal = devis.getId() != null ? devis.total() : null;

    devis.remplacerLignes(request.lignes().stream()
        .map(l -> new DevisLigne(l.type(), l.libelle().trim(),
            l.quantite() != null ? l.quantite() : BigDecimal.ONE, l.prixUnitaire()))
        .toList());
    devis.setCommentaire(request.commentaire() == null || request.commentaire().isBlank()
        ? null : request.commentaire().trim());
    devis.setStatut(StatutDevis.EN_ATTENTE);
    devis.setDecideAt(null);
    devis = devisRepository.save(devis);

    evenementService.journaliser(dossier, TypeActionEvenement.DEVIS,
        ancienTotal != null ? "Devis : " + Libelles.montant(ancienTotal) : null,
        "Devis : " + Libelles.montant(devis.total()), null);

    if (List.of(StatutDossier.RECU, StatutDossier.DIAGNOSTIC).contains(dossier.getStatut())) {
      dossierService.appliquerStatut(dossier, StatutDossier.ATTENTE_CLIENT, "Devis en attente de réponse du client");
    }
    return DevisDto.of(devis);
  }

  /** Réponse du client transmise par un employé. */
  public DevisDto decider(Long dossierId, boolean accepte, String commentaire) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    return DevisDto.of(appliquerDecision(dossier, accepte, commentaire, dossierService.employeConnecte()));
  }

  /** Réponse donnée par le client lui-même depuis le suivi en ligne (auteur null dans l'historique). */
  public DevisDto deciderParClient(Dossier dossier, boolean accepte) {
    if (dossier.estCloture()) {
      throw new ConflictException("Ce dossier est terminé");
    }
    return DevisDto.of(appliquerDecision(dossier, accepte, "Réponse donnée en ligne par le client", null));
  }

  public void supprimer(Long dossierId) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Devis devis = getOrThrow(dossier);
    if (devis.getStatut() == StatutDevis.ACCEPTE) {
      throw new ConflictException("Un devis accepté ne peut pas être supprimé");
    }
    devisRepository.delete(devis);
    evenementService.journaliser(dossier, TypeActionEvenement.DEVIS,
        "Devis : " + Libelles.montant(devis.total()), "Devis supprimé", null);
  }

  public void envoyerParEmail(Long dossierId) {
    Dossier dossier = dossierService.getModifiableOrThrow(dossierId);
    Devis devis = getOrThrow(dossier);
    if (devis.getStatut() != StatutDevis.EN_ATTENTE) {
      throw new ConflictException("Le client a déjà répondu à ce devis");
    }
    notificationService.envoyerDevis(dossier, devis);
  }

  private Devis appliquerDecision(Dossier dossier, boolean accepte, String commentaire, Employe auteur) {
    Devis devis = getOrThrow(dossier);
    if (devis.getStatut() != StatutDevis.EN_ATTENTE) {
      throw new ConflictException("Le client a déjà répondu à ce devis");
    }
    devis.setStatut(accepte ? StatutDevis.ACCEPTE : StatutDevis.REFUSE);
    devis.setDecideAt(OffsetDateTime.now(clock));

    evenementService.journaliserPar(auteur, dossier, TypeActionEvenement.DECISION_DEVIS,
        "Devis : " + Libelles.montant(devis.total()), accepte ? "Devis accepté" : "Devis refusé",
        commentaire == null || commentaire.isBlank() ? null : commentaire.trim());

    if (accepte && dossier.getStatut() == StatutDossier.ATTENTE_CLIENT
        && StatutDossierWorkflow.peutPasser(dossier.getStatut(), StatutDossier.REPARATION)) {
      dossierService.appliquerStatut(dossier, StatutDossier.REPARATION, "Devis accepté", auteur);
    }
    return devis;
  }

  private Devis getOrThrow(Dossier dossier) {
    return devisRepository.findByDossier(dossier)
        .orElseThrow(() -> new ResourceNotFoundException("Aucun devis pour le dossier " + dossier.getReference()));
  }
}

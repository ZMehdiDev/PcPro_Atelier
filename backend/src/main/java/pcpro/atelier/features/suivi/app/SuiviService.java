package pcpro.atelier.features.suivi.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.core.exception.ResourceNotFoundException;
import pcpro.atelier.core.exception.TooManyRequestsException;
import pcpro.atelier.core.security.LoginAttemptService;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.devis.api.dto.DevisDto;
import pcpro.atelier.features.devis.app.DevisService;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.infra.DevisRepository;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.paiement.app.SoldeService;
import pcpro.atelier.features.suivi.api.dto.SuiviDto;

/**
 * Suivi en ligne par le client, sans compte : référence du dossier + code de suivi (QR code du bon de dépôt,
 * lien des emails) ou numéro de téléphone. Les essais ratés sont limités comme les connexions.
 *
 * Seul l'accès par code permet de répondre au devis (le téléphone d'un client peut être connu d'un tiers).
 */
@Service
@Transactional
public class SuiviService {

  private static final String INTROUVABLE = "Aucun dossier ne correspond, vérifiez la référence et le code";

  private final DossierRepository dossierRepository;
  private final DevisRepository devisRepository;
  private final DevisService devisService;
  private final SoldeService soldeService;
  private final LoginAttemptService tentatives;
  private final AtelierProperties atelier;

  public SuiviService(
      DossierRepository dossierRepository,
      DevisRepository devisRepository,
      DevisService devisService,
      SoldeService soldeService,
      LoginAttemptService tentatives,
      AtelierProperties atelier
  ) {
    this.dossierRepository = dossierRepository;
    this.devisRepository = devisRepository;
    this.devisService = devisService;
    this.soldeService = soldeService;
    this.tentatives = tentatives;
    this.atelier = atelier;
  }

  @Transactional(readOnly = true)
  public SuiviDto consulter(String reference, String code, String telephone, String ip) {
    boolean parCode = code != null && !code.isBlank();
    Dossier dossier = verifierAcces(reference, ip,
        d -> parCode ? memeCode(d, code) : memeTelephone(d.getClient().getTelephone(), telephone));
    return toDto(dossier, parCode);
  }

  public SuiviDto repondreDevis(String reference, String code, boolean accepte, String ip) {
    Dossier dossier = verifierAcces(reference, ip, d -> memeCode(d, code));
    devisService.deciderParClient(dossier, accepte);
    return toDto(dossier, true);
  }

  private Dossier verifierAcces(String referenceSaisie, String ip, Predicate<Dossier> preuve) {
    String reference = referenceSaisie == null ? "" : referenceSaisie.trim().toUpperCase(Locale.ROOT);
    String cle = "suivi:" + reference;
    if (tentatives.estBloque(cle, ip)) {
      throw new TooManyRequestsException("Trop d'essais, réessayez dans quelques minutes");
    }
    Dossier dossier = dossierRepository.findByReference(reference).filter(preuve).orElse(null);
    if (dossier == null) {
      tentatives.echec(cle, ip);
      throw new ResourceNotFoundException(INTROUVABLE);
    }
    tentatives.succes(cle, ip);
    return dossier;
  }

  private SuiviDto toDto(Dossier dossier, boolean parCode) {
    Devis devis = devisRepository.findByDossier(dossier).orElse(null);
    return new SuiviDto(
        dossier.getReference(),
        Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele()),
        dossier.getStatut(),
        Libelles.statutClient(dossier.getStatut()),
        dossier.getDateEntree(),
        dossier.getDateEstimee(),
        dossier.getPretAt(),
        dossier.getFinGarantie(),
        DevisDto.of(devis),
        parCode && devis != null && devis.getStatut() == StatutDevis.EN_ATTENTE && !dossier.estCloture(),
        soldeService.resteAPayer(dossier),
        new SuiviDto.Atelier(atelier.nom(), atelier.adresse(), atelier.telephone(), atelier.horaires())
    );
  }

  private static boolean memeCode(Dossier dossier, String code) {
    if (code == null) {
      return false;
    }
    byte[] attendu = dossier.getCodeSuivi().getBytes(StandardCharsets.UTF_8);
    byte[] saisi = code.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(attendu, saisi);
  }

  /** Compare les 9 derniers chiffres : "0470 12 34 56" et "+32 470 12 34 56" sont le même numéro. */
  static boolean memeTelephone(String telephoneClient, String saisi) {
    if (telephoneClient == null || saisi == null) {
      return false;
    }
    String a = telephoneClient.replaceAll("[^0-9]", "");
    String b = saisi.replaceAll("[^0-9]", "");
    if (a.length() < 8 || b.length() < 8) {
      return false;
    }
    return a.substring(Math.max(0, a.length() - 9)).equals(b.substring(Math.max(0, b.length() - 9)));
  }
}

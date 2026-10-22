package pcpro.atelier.features.suivi.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.features.devis.api.dto.DevisDto;

/**
 * Ce que le client voit de son dossier : pas de notes internes, pas d'historique détaillé.
 *
 * @param peutRepondreDevis le client peut accepter ou refuser le devis en ligne (accès par code de suivi)
 */
public record SuiviDto(
    String reference,
    String appareil,
    StatutDossier statut,
    String statutLibelle,
    LocalDate dateEntree,
    LocalDate dateEstimee,
    OffsetDateTime pretDepuis,
    LocalDate finGarantie,
    DevisDto devis,
    boolean peutRepondreDevis,
    BigDecimal resteAPayer,
    Atelier atelier
) {

  public record Atelier(String nom, String adresse, String telephone, String horaires) {
  }
}

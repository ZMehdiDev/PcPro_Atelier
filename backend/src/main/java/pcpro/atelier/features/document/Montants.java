package pcpro.atelier.features.document;

import java.math.BigDecimal;
import java.math.RoundingMode;

import pcpro.atelier.core.config.AtelierProperties.RegimeTva;
import pcpro.atelier.core.enums.TypeLigneDevis;

/**
 * Lignes et totaux imprimés sur les devis et factures.
 */
public final class Montants {

  private Montants() {
  }

  /** Une ligne chiffrée (prix TVAC, comme saisis dans l'appli). */
  public record Ligne(String libelle, String detail, BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal total) {
  }

  /**
   * Total d'un document. Assujetti : la TVA est extraite du total TVAC (prix saisis TVAC).
   * En franchise : pas de TVA, HTVA = TVAC.
   */
  public record Totaux(RegimeTva regime, BigDecimal taux, BigDecimal htva, BigDecimal tva, BigDecimal tvac) {

    public static Totaux of(BigDecimal tvac, RegimeTva regime, BigDecimal taux) {
      if (regime == RegimeTva.FRANCHISE || taux.signum() == 0) {
        return new Totaux(regime, taux, tvac, BigDecimal.ZERO, tvac);
      }
      BigDecimal htva = tvac.multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(100).add(taux), 2, RoundingMode.HALF_UP);
      return new Totaux(regime, taux, htva, tvac.subtract(htva), tvac);
    }

    public boolean assujetti() {
      return regime == RegimeTva.ASSUJETTI;
    }
  }

  /** Libellé lisible du type d'une ligne de devis. */
  public static String type(TypeLigneDevis type) {
    return switch (type) {
      case MAIN_OEUVRE -> "Main-d'œuvre";
      case PIECE -> "Pièce";
      case AUTRE -> null;
    };
  }

  /** Mention légale (texte exact exigé par le SPF Finances) sur les factures d'une entreprise en franchise de TVA. */
  public static final String MENTION_FRANCHISE = "Régime particulier de franchise des petites entreprises";
}

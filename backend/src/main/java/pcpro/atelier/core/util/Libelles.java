package pcpro.atelier.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.enums.TypeAppareil;

/**
 * Textes lisibles par un client (emails, bon de dépôt, suivi en ligne).
 */
public final class Libelles {

  public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

  private Libelles() {
  }

  /** Ex : "ordinateur portable Lenovo ThinkPad T14". */
  public static String appareil(TypeAppareil type, String marque, String modele) {
    String nature = switch (type) {
      case PORTABLE -> "ordinateur portable";
      case TOUR -> "ordinateur";
      case AUTRE -> "appareil";
    };
    return Stream.of(nature, marque, modele)
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(" "));
  }

  /** Statut tel qu'expliqué au client. */
  public static String statutClient(StatutDossier statut) {
    return switch (statut) {
      case RECU -> "Appareil reçu, en attente de diagnostic";
      case DIAGNOSTIC -> "Diagnostic en cours";
      case ATTENTE_CLIENT -> "En attente de votre réponse";
      case ATTENTE_PIECE -> "En attente de pièces";
      case REPARATION -> "Réparation en cours";
      case PRET -> "Prêt, vous pouvez venir le récupérer";
      case CLOTURE -> "Récupéré";
    };
  }

  /** Ex : "45,00 €". */
  public static String montant(BigDecimal montant) {
    return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
  }
}

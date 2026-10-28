package pcpro.atelier.features.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mise en forme des valeurs imprimées (à la belge : "1 234,50 €", "28/10/2026"). Utilisé dans les gabarits via "fmt".
 */
public final class Formats {

  public static final Formats INSTANCE = new Formats();

  private static final ZoneId BRUXELLES = ZoneId.of("Europe/Brussels");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
  private static final DateTimeFormatter DATE_LONGUE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
  private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

  private Formats() {
  }

  /** Jour d'un instant, à l'heure de Bruxelles. */
  public static LocalDate jour(OffsetDateTime instant) {
    return instant == null ? null : instant.atZoneSameInstant(BRUXELLES).toLocalDate();
  }

  /** Ex : "1 234,50 €" (espace fine insécable entre les milliers). Null : "". */
  public String montant(BigDecimal montant) {
    if (montant == null) {
      return "";
    }
    return nombre(montant, 2) + " €";
  }

  /** Comme montant, mais un tiret pour zéro (tableaux où les zéros noieraient les vrais montants). */
  public String montantOuTiret(BigDecimal montant) {
    return montant == null || montant.signum() == 0 ? "–" : montant(montant);
  }

  /** Quantité sans décimales inutiles : "1", "1,5". */
  public String quantite(BigDecimal quantite) {
    if (quantite == null) {
      return "";
    }
    BigDecimal q = quantite.stripTrailingZeros();
    return nombre(q, Math.max(0, q.scale()));
  }

  public String quantite(int quantite) {
    return String.valueOf(quantite);
  }

  /** Ex : "21" ou "6". */
  public String taux(BigDecimal taux) {
    return quantite(taux);
  }

  public String date(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }

  public String date(OffsetDateTime date) {
    return date == null ? "" : DATE.format(date.atZoneSameInstant(BRUXELLES));
  }

  /** Ex : "28 octobre 2026". */
  public String dateLongue(LocalDate date) {
    return date == null ? "" : DATE_LONGUE.format(date);
  }

  public String heure(OffsetDateTime date) {
    return date == null ? "" : HEURE.format(date.atZoneSameInstant(BRUXELLES));
  }

  /** "BE68 5390 0754 7034" : IBAN par groupes de 4. */
  public String iban(String iban) {
    if (iban == null) {
      return "";
    }
    return iban.replaceAll("(.{4})(?!$)", "$1 ");
  }

  /** Première lettre en majuscule. */
  public String majuscule(String texte) {
    if (texte == null || texte.isEmpty()) {
      return "";
    }
    return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
  }

  public boolean vide(String texte) {
    return texte == null || texte.isBlank();
  }

  private static String nombre(BigDecimal valeur, int decimales) {
    DecimalFormatSymbols symboles = DecimalFormatSymbols.getInstance(Locale.FRENCH);
    symboles.setDecimalSeparator(',');
    symboles.setGroupingSeparator(' ');
    symboles.setMinusSign('−');
    DecimalFormat format = new DecimalFormat("#,##0" + (decimales > 0 ? "." + "0".repeat(decimales) : ""), symboles);
    return format.format(valeur.setScale(decimales, RoundingMode.HALF_UP));
  }
}

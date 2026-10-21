package pcpro.atelier.core.util;

/**
 * Normalise un numéro de téléphone pour qu'un même client ne soit pas créé deux fois
 * ("0470 12 34 56", "0470/12.34.56" et "0470123456" donnent le même numéro).
 */
public final class TelephoneNormalizer {

  private TelephoneNormalizer() {
  }

  /**
   * Garde uniquement les chiffres, et un "+" s'il est en tête. Renvoie null si l'entrée est vide.
   */
  public static String normaliser(String telephone) {
    if (telephone == null) {
      return null;
    }
    String trimmed = telephone.trim();
    String chiffres = trimmed.replaceAll("[^0-9]", "");
    if (chiffres.isEmpty()) {
      return null;
    }
    return trimmed.startsWith("+") ? "+" + chiffres : chiffres;
  }
}

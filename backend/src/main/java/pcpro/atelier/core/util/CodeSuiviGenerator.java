package pcpro.atelier.core.util;

import java.security.SecureRandom;

/**
 * Code secret du suivi en ligne : 8 caractères sans ambiguïté à la lecture (pas de 0/O, 1/I/L),
 * soit environ 10^12 combinaisons, en plus de la limite de tentatives.
 */
public final class CodeSuiviGenerator {

  private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int LONGUEUR = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private CodeSuiviGenerator() {
  }

  public static String generer() {
    StringBuilder code = new StringBuilder(LONGUEUR);
    for (int i = 0; i < LONGUEUR; i++) {
      code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}

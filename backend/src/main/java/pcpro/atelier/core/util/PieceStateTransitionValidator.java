package pcpro.atelier.core.util;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.core.exception.ConflictException;

/**
 * Valide les transitions d'état autorisées pour les pièces.
 *
 * Transitions autorisées :
 * - A_COMMANDER -> COMMANDEE, ANNULEE
 * - COMMANDEE -> RECUE, ANNULEE
 * - RECUE -> MONTEE, ANNULEE
 * - MONTEE -> (état final, pas de transition)
 * - ANNULEE -> (état final, pas de transition)
 */
public final class PieceStateTransitionValidator {

  private static final Map<EtatPiece, Set<EtatPiece>> TRANSITIONS_AUTORISEES = Map.of(
      EtatPiece.A_COMMANDER, EnumSet.of(EtatPiece.COMMANDEE, EtatPiece.ANNULEE),
      EtatPiece.COMMANDEE, EnumSet.of(EtatPiece.RECUE, EtatPiece.ANNULEE),
      EtatPiece.RECUE, EnumSet.of(EtatPiece.MONTEE, EtatPiece.ANNULEE),
      EtatPiece.MONTEE, EnumSet.noneOf(EtatPiece.class),
      EtatPiece.ANNULEE, EnumSet.noneOf(EtatPiece.class)
  );

  private PieceStateTransitionValidator() {
  }

  /**
   * @throws ConflictException si la transition n'est pas autorisée
   */
  public static void validerTransition(EtatPiece ancienEtat, EtatPiece nouvelEtat) {
    if (ancienEtat == nouvelEtat) {
      throw new ConflictException("La pièce est déjà dans l'état " + nouvelEtat);
    }

    Set<EtatPiece> transitionsAutorisees = TRANSITIONS_AUTORISEES.get(ancienEtat);
    if (!transitionsAutorisees.contains(nouvelEtat)) {
      throw new ConflictException(String.format(
          "Transition non autorisée : %s -> %s. Transitions autorisées depuis %s : %s",
          ancienEtat,
          nouvelEtat,
          ancienEtat,
          transitionsAutorisees.isEmpty() ? "aucune (état final)" : transitionsAutorisees
      ));
    }
  }

  public static Set<EtatPiece> transitionsPossibles(EtatPiece etat) {
    return EnumSet.copyOf(TRANSITIONS_AUTORISEES.get(etat));
  }

  public static boolean estEtatFinal(EtatPiece etat) {
    return TRANSITIONS_AUTORISEES.get(etat).isEmpty();
  }
}

package pcpro.atelier.core.util;

import pcpro.atelier.core.enums.EtatPiece;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
public class PieceStateTransitionValidator {

  private static final Map<EtatPiece, Set<EtatPiece>> TRANSITIONS_AUTORISEES = Map.of(
      EtatPiece.A_COMMANDER, EnumSet.of(EtatPiece.COMMANDEE, EtatPiece.ANNULEE),
      EtatPiece.COMMANDEE, EnumSet.of(EtatPiece.RECUE, EtatPiece.ANNULEE),
      EtatPiece.RECUE, EnumSet.of(EtatPiece.MONTEE, EtatPiece.ANNULEE),
      EtatPiece.MONTEE, EnumSet.noneOf(EtatPiece.class), // État final
      EtatPiece.ANNULEE, EnumSet.noneOf(EtatPiece.class) // État final
  );

  /**
   * Valide qu'une transition d'état est autorisée.
   * 
   * @param ancienEtat L'état actuel de la pièce
   * @param nouvelEtat Le nouvel état souhaité
   * @throws IllegalArgumentException si la transition n'est pas autorisée
   */
  public static void validerTransition(EtatPiece ancienEtat, EtatPiece nouvelEtat) {
    if (ancienEtat == nouvelEtat) {
      throw new IllegalArgumentException(
          "La pièce est déjà dans l'état " + nouvelEtat + ". Aucun changement nécessaire."
      );
    }

    Set<EtatPiece> transitionsAutorisees = TRANSITIONS_AUTORISEES.get(ancienEtat);
    
    if (transitionsAutorisees == null) {
      throw new IllegalArgumentException(
          "État de pièce inconnu : " + ancienEtat
      );
    }

    if (!transitionsAutorisees.contains(nouvelEtat)) {
      throw new IllegalArgumentException(
          String.format(
              "Transition non autorisée : %s -> %s. Transitions autorisées depuis %s : %s",
              ancienEtat,
              nouvelEtat,
              ancienEtat,
              transitionsAutorisees.isEmpty() ? "aucune (état final)" : transitionsAutorisees
          )
      );
    }
  }

  /**
   * Vérifie si un état est un état final (pas de transition possible).
   */
  public static boolean estEtatFinal(EtatPiece etat) {
    Set<EtatPiece> transitions = TRANSITIONS_AUTORISEES.get(etat);
    return transitions != null && transitions.isEmpty();
  }
}

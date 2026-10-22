package pcpro.atelier.core.util;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.exception.ConflictException;

/**
 * Cycle de vie d'un dossier de réparation.
 *
 * RECU -> DIAGNOSTIC -> (ATTENTE_CLIENT | ATTENTE_PIECE) -> REPARATION -> PRET -> CLOTURE
 *
 * Les retours en arrière utiles à l'atelier sont permis (ex : PRET -> REPARATION si le client revient
 * avant de récupérer ou si un dernier test échoue). Un appareil peut aussi être rendu
 * sans réparation (RECU/DIAGNOSTIC/ATTENTE_CLIENT -> PRET). CLOTURE -> DIAGNOSTIC est la réouverture,
 * réservée aux administrateurs (contrôlée par le service).
 */
public final class StatutDossierWorkflow {

  private static final Map<StatutDossier, Set<StatutDossier>> TRANSITIONS = Map.of(
      StatutDossier.RECU, EnumSet.of(
          StatutDossier.DIAGNOSTIC, StatutDossier.ATTENTE_CLIENT, StatutDossier.PRET),
      StatutDossier.DIAGNOSTIC, EnumSet.of(
          StatutDossier.ATTENTE_CLIENT, StatutDossier.ATTENTE_PIECE, StatutDossier.REPARATION, StatutDossier.PRET),
      StatutDossier.ATTENTE_CLIENT, EnumSet.of(
          StatutDossier.DIAGNOSTIC, StatutDossier.ATTENTE_PIECE, StatutDossier.REPARATION, StatutDossier.PRET),
      StatutDossier.ATTENTE_PIECE, EnumSet.of(
          StatutDossier.DIAGNOSTIC, StatutDossier.ATTENTE_CLIENT, StatutDossier.REPARATION),
      StatutDossier.REPARATION, EnumSet.of(
          StatutDossier.ATTENTE_CLIENT, StatutDossier.ATTENTE_PIECE, StatutDossier.PRET),
      StatutDossier.PRET, EnumSet.of(
          StatutDossier.REPARATION, StatutDossier.CLOTURE),
      StatutDossier.CLOTURE, EnumSet.of(
          StatutDossier.DIAGNOSTIC)
  );

  private StatutDossierWorkflow() {
  }

  public static boolean peutPasser(StatutDossier actuel, StatutDossier cible) {
    return TRANSITIONS.get(actuel).contains(cible);
  }

  public static Set<StatutDossier> transitionsPossibles(StatutDossier actuel) {
    return EnumSet.copyOf(TRANSITIONS.get(actuel));
  }

  /**
   * @throws ConflictException si la transition n'est pas autorisée
   */
  public static void validerTransition(StatutDossier actuel, StatutDossier cible) {
    if (actuel == cible) {
      throw new ConflictException("Le dossier est déjà au statut " + cible);
    }
    if (!peutPasser(actuel, cible)) {
      throw new ConflictException(String.format(
          "Transition non autorisée : %s -> %s. Statuts possibles depuis %s : %s",
          actuel, cible, actuel, TRANSITIONS.get(actuel)
      ));
    }
  }
}

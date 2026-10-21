package pcpro.atelier.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pcpro.atelier.core.enums.EtatPiece;
import pcpro.atelier.core.exception.ConflictException;

class PieceStateTransitionValidatorTest {

  @Test
  void cycleNormal() {
    assertThatCode(() -> {
      PieceStateTransitionValidator.validerTransition(EtatPiece.A_COMMANDER, EtatPiece.COMMANDEE);
      PieceStateTransitionValidator.validerTransition(EtatPiece.COMMANDEE, EtatPiece.RECUE);
      PieceStateTransitionValidator.validerTransition(EtatPiece.RECUE, EtatPiece.MONTEE);
    }).doesNotThrowAnyException();
  }

  @Test
  void etatsFinaux() {
    assertThat(PieceStateTransitionValidator.estEtatFinal(EtatPiece.MONTEE)).isTrue();
    assertThat(PieceStateTransitionValidator.estEtatFinal(EtatPiece.ANNULEE)).isTrue();
    assertThatThrownBy(() -> PieceStateTransitionValidator.validerTransition(EtatPiece.MONTEE, EtatPiece.ANNULEE))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void sautDEtapeRefuse() {
    assertThatThrownBy(() -> PieceStateTransitionValidator.validerTransition(EtatPiece.A_COMMANDER, EtatPiece.MONTEE))
        .isInstanceOf(ConflictException.class);
  }
}

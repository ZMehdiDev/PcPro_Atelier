package pcpro.atelier.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.exception.ConflictException;

class StatutDossierWorkflowTest {

  @Test
  void chaqueStatutAAuMoinsUneSortie() {
    for (StatutDossier statut : StatutDossier.values()) {
      assertThat(StatutDossierWorkflow.transitionsPossibles(statut)).isNotEmpty();
    }
  }

  @Test
  void clotureUniquementDepuisPret() {
    for (StatutDossier statut : StatutDossier.values()) {
      assertThat(StatutDossierWorkflow.peutPasser(statut, StatutDossier.CLOTURE))
          .isEqualTo(statut == StatutDossier.PRET);
    }
  }

  @Test
  void transitionVersLeMemeStatutRefusee() {
    assertThatThrownBy(() -> StatutDossierWorkflow.validerTransition(StatutDossier.RECU, StatutDossier.RECU))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void transitionNonPrevueRefusee() {
    assertThatThrownBy(() -> StatutDossierWorkflow.validerTransition(StatutDossier.RECU, StatutDossier.TESTS))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("RECU -> TESTS");
  }
}

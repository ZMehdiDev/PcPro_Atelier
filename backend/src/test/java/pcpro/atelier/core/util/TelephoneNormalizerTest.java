package pcpro.atelier.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TelephoneNormalizerTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', nullValues = "null", value = {
      "0470 12 34 56|0470123456",
      "0470/12.34.56|0470123456",
      "+32 470 12 34 56|+32470123456",
      " +32-470-123456 |+32470123456",
      "abc|null",
      "null|null"
  })
  void normalise(String entree, String attendu) {
    assertThat(TelephoneNormalizer.normaliser(entree)).isEqualTo(attendu);
  }
}

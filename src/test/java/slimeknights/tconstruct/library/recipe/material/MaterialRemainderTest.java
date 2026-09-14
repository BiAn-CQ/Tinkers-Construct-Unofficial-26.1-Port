package slimeknights.tconstruct.library.recipe.material;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MaterialRemainderTest {
  private IMaterialValue value(int value, int needed) {
    IMaterialValue material = mock(IMaterialValue.class, CALLS_REAL_METHODS);
    when(material.getValue()).thenReturn(value);
    when(material.getNeeded()).thenReturn(needed);
    return material;
  }

  @Test
  void remainderIsUnusedMaterialRatherThanConsumedFraction() {
    IMaterialValue block = value(9, 1);
    assertThat(block.getRemainder(1)).isEqualTo(8);
    assertThat(block.getRemainder(9)).isZero();
    assertThat(block.getRemainder(10)).isEqualTo(8);
  }

  @Test
  void remainderAccountsForRequiredInputCountAndZeroValue() {
    assertThat(value(9, 2).getRemainder(5)).isEqualTo(8);
    assertThat(value(0, 1).getRemainder(5)).isZero();
  }
}

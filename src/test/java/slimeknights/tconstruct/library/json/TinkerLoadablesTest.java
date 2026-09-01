package slimeknights.tconstruct.library.json;

import com.google.gson.JsonSyntaxException;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.typed.TypedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TinkerLoadablesTest {
  @Test
  void operationUsesNativeNames() {
    assertThat(TinkerLoadables.OPERATION.parseString("add_value", "operation", TypedMap.EMPTY))
      .isEqualTo(Operation.ADD_VALUE);
    assertThat(TinkerLoadables.OPERATION.parseString("add_multiplied_base", "operation", TypedMap.EMPTY))
      .isEqualTo(Operation.ADD_MULTIPLIED_BASE);
    assertThat(TinkerLoadables.OPERATION.parseString("add_multiplied_total", "operation", TypedMap.EMPTY))
      .isEqualTo(Operation.ADD_MULTIPLIED_TOTAL);
    assertThatThrownBy(() -> TinkerLoadables.OPERATION.parseString("add", "operation", TypedMap.EMPTY))
      .isInstanceOf(JsonSyntaxException.class);
  }
}

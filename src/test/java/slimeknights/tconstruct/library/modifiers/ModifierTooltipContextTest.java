package slimeknights.tconstruct.library.modifiers;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.impl.ComposableModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierTooltip;
import static org.assertj.core.api.Assertions.assertThat;

class ModifierTooltipContextTest {
  @Test
  void newFieldCanHideModifierInAllContexts() {
    var modifier = ComposableModifier.LOADER.convert(JsonParser.parseString("{\"show_in_tooltips\":\"never\"}"), "modifier");
    for (var context : ModifierTooltip.values()) {
      assertThat(modifier.shouldDisplay(context)).isFalse();
    }
  }

  @Test
  void legacyNeverStillAllowsPartAndBookContexts() {
    var modifier = ComposableModifier.LOADER.convert(JsonParser.parseString("{\"tooltip_display\":\"never\"}"), "modifier");
    assertThat(modifier.shouldDisplay(ModifierTooltip.TOOL)).isFalse();
    assertThat(modifier.shouldDisplay(ModifierTooltip.TINKER_STATION)).isFalse();
    assertThat(modifier.shouldDisplay(ModifierTooltip.TOOL_PART)).isTrue();
    assertThat(modifier.shouldDisplay(ModifierTooltip.BOOK)).isTrue();
  }
}

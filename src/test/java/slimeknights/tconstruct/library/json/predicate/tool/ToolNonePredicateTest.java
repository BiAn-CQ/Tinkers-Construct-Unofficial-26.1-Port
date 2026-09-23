package slimeknights.tconstruct.library.json.predicate.tool;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ToolNonePredicateTest extends BaseMcTest {
  @Test
  void contextNoneRemainsFalseThroughLoadingAndComposition() {
    var none = ToolContextPredicate.LOADER.convert(new JsonPrimitive("mantle:none"), "predicate");
    var tool = mock(IToolContext.class);
    assertThat(none.matches(tool)).isFalse();
    assertThat(none.inverted().matches(tool)).isTrue();
    assertThat(ToolContextPredicate.and(ToolContextPredicate.ANY, none).matches(tool)).isFalse();
    assertThat(ToolContextPredicate.or(none, none).matches(tool)).isFalse();
  }

  @Test
  void stackNoneRemainsFalseThroughLoadingAndComposition() {
    var none = ToolStackPredicate.LOADER.convert(new JsonPrimitive("mantle:none"), "predicate");
    var tool = mock(IToolStackView.class);
    assertThat(none.matches(tool)).isFalse();
    assertThat(none.inverted().matches(tool)).isTrue();
    assertThat(ToolStackPredicate.and(ToolStackPredicate.ANY, none).matches(tool)).isFalse();
    assertThat(ToolStackPredicate.or(none, none).matches(tool)).isFalse();
  }
}

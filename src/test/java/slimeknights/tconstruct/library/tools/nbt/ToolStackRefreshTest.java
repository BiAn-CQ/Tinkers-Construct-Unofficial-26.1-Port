package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerTools;

import static org.assertj.core.api.Assertions.assertThat;

class ToolStackRefreshTest extends BaseMcTest {
  @Test
  void rawDataAccessKeepsChangesMadeThroughAnotherStackView() {
    ItemStack stack = new ItemStack(TinkerTools.pickaxe.get());
    ToolStack tool = ToolStack.from(stack);
    tool.getRestrictedNBT().putString("test:first", "first");
    ItemStackDataUtil.updateTag(stack, tag -> tag.putString("test:external", "external"));
    tool.getRestrictedNBT().putString("test:last", "last");
    var saved = ItemStackDataUtil.getTag(stack);
    assertThat(saved.getStringOr("test:external", "")).isEqualTo("external");
    assertThat(saved.getStringOr("test:first", "")).isEqualTo("first");
    assertThat(saved.getStringOr("test:last", "")).isEqualTo("last");
  }
}

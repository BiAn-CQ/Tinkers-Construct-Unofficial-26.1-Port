package slimeknights.tconstruct.library.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ModifierDisplayPersistentDataTest extends slimeknights.tconstruct.test.CoreTestBootstrap {
  @Test
  void displayStoresPersistentDataBeforeCopyingComponent() {
    ItemStack original = new ItemStack(Items.STICK);
    ItemStack result = IDisplayModifierRecipe.withModifiers(original, List.of(),
      data -> data.putString(Identifier.fromNamespaceAndPath("tconstruct", "display_test"), "variant"));
    assertThat(ItemStackDataUtil.getTag(result).getCompoundOrEmpty(ToolStack.TAG_PERSISTENT_MOD_DATA)
      .getStringOr("tconstruct:display_test", "")).isEqualTo("variant");
    assertThat(ItemStackDataUtil.getTag(original)).isNull();
  }
}

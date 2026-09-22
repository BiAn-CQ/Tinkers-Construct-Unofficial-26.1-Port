package slimeknights.tconstruct.plugin.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.modifiers.hook.build.CraftCountModifierHook;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ToolBuildingDisplayTest extends slimeknights.tconstruct.test.CoreTestBootstrap {
  @Test
  void displayedOutputStoresPartsAndExtraMaterialsInOrder() {
    MaterialId iron = new MaterialId("tconstruct", "iron");
    MaterialId wood = new MaterialId("tconstruct", "wood");
    ItemStack output = new MaterialIdNBT(List.of(iron, wood)).updateStack(new ItemStack(Items.STICK));
    assertThat(MaterialIdNBT.from(output).getMaterials()).containsExactly(iron, wood);
  }

  @Test
  void displayCopyRetainsExtraMaterialsAndRespectsUnstackableTools() {
    MaterialId iron = new MaterialId("tconstruct", "iron");
    MaterialId wood = new MaterialId("tconstruct", "wood");
    ItemStack output = CraftCountModifierHook.copyMaterials(
      new MaterialIdNBT(List.of(iron, wood)), Items.DIAMOND_SWORD, 16);
    assertThat(MaterialIdNBT.from(output).getMaterials()).containsExactly(iron, wood);
    assertThat(output.getCount()).isEqualTo(1);
  }
}

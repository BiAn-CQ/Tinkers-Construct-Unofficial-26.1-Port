package slimeknights.tconstruct.plugin.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import java.nio.file.Files;
import java.nio.file.Path;
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
  void categoryWiresDynamicPartsToRecipeExtraMaterials() throws Exception {
    // JEI and Minecraft live in separate test classloaders; inspect the callback wiring
    // without loading the client category, and test its material representation above.
    Path relative = Path.of("src/main/java/slimeknights/tconstruct/plugin/jei/ToolBuildingCategory.java");
    Path root = Path.of("").toAbsolutePath();
    while (root != null && !Files.isRegularFile(root.resolve(relative))) root = root.getParent();
    assertThat(root).as("repository containing category source").isNotNull();
    String source = Files.readString(root.resolve(relative));
    assertThat(source).contains("public void onDisplayedIngredientsUpdate(", "variants.addAll(recipe.getExtraMaterials())",
      "getMaterial(inputs.get(i).getDisplayedItemStack()", ".setSlotName(RESULT_SLOT)");
  }
}

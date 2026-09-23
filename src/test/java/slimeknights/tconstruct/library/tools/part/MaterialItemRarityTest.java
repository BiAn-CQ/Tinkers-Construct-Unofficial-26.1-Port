package slimeknights.tconstruct.library.tools.part;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MaterialItemRarityTest extends BaseMcTest {
  @BeforeAll
  static void initItems() {
    MaterialItemFixture.init();
  }

  @Test
  void existingPartReflectsMaterialChangesWithoutChangingStackComponents() {
    MaterialId id = new MaterialId("test", "rarity");
    ItemStack stack = MaterialItemFixture.MATERIAL_ITEM.withMaterialForDisplay(id);
    stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
    ItemStack original = stack.copy();
    IMaterial material = mock(IMaterial.class);
    when(material.getRarity()).thenReturn(Rarity.RARE, Rarity.EPIC);
    try (MockedStatic<MaterialRegistry> registry = mockStatic(MaterialRegistry.class)) {
      registry.when(() -> MaterialRegistry.getMaterial(id)).thenReturn(material);
      assertThat(stack.getRarity()).isEqualTo(Rarity.RARE);
      assertThat(stack.getRarity()).isEqualTo(Rarity.EPIC);
    }
    assertThat(ItemStack.isSameItemSameComponents(stack, original)).isTrue();
  }

  @Test
  void existingToolReflectsVolatileRarityWithoutChangingComponentsOnRead() {
    ItemStack stack = new ItemStack(TinkerTools.pickaxe.get());
    stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
    for (Rarity rarity : new Rarity[]{Rarity.RARE, Rarity.EPIC, Rarity.COMMON}) {
      ItemStackDataUtil.updateTagElement(stack, ToolStack.TAG_VOLATILE_MOD_DATA,
        tag -> tag.putInt(RarityModule.RARITY.toString(), rarity.ordinal()));
      ItemStack original = stack.copy();
      assertThat(stack.getRarity()).isEqualTo(rarity);
      assertThat(ItemStack.isSameItemSameComponents(stack, original)).isTrue();
    }
  }

  @Test
  void ordinaryItemRetainsItsComponentRarity() {
    ItemStack stack = new ItemStack(Items.STICK);
    stack.set(DataComponents.RARITY, Rarity.RARE);
    assertThat(stack.getRarity()).isEqualTo(Rarity.RARE);
  }
}

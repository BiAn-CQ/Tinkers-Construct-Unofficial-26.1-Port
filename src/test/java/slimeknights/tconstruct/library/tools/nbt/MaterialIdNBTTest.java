package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialIdNBTTest extends BaseMcTest {
  @Test
  void invalidMaterialRetainsItsSlotInsteadOfShiftingLaterParts() {
    ListTag list = new ListTag();
    list.add(StringTag.valueOf("Invalid Material"));
    list.add(StringTag.valueOf("tconstruct:iron"));
    MaterialIdNBT materials = MaterialIdNBT.readFromNBT(list);
    assertThat(materials.size()).isEqualTo(2);
    assertThat(materials.getMaterial(0)).isEqualTo(MaterialId.UNKNOWN);
    assertThat(materials.getMaterial(1)).isEqualTo(new MaterialId("tconstruct:iron"));
  }

  @Test
  void singleMaterialLookupReadsThePortDataComponent() {
    ListTag list = new ListTag();
    list.add(StringTag.valueOf("tconstruct:iron"));
    ItemStack stack = new ItemStack(Items.STICK);
    ItemStackDataUtil.updateTag(stack, tag -> tag.put(ToolStack.TAG_MATERIALS, list));
    assertThat(MaterialIdNBT.getMaterial(stack, 0)).isEqualTo(new MaterialId("tconstruct:iron"));
    assertThat(MaterialIdNBT.getMaterial(stack, 1)).isEqualTo(MaterialId.UNKNOWN);
  }
}

package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

class PotionFluidTypeTest extends BaseMcTest {
  @Test
  void readsNativePotionContentsFromFluid() {
    FluidStack stack = new FluidStack(Fluids.WATER, 50);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.SWIFTNESS));

    assertThat(PotionFluidType.getPotionContents(stack).potion()).contains(Potions.SWIFTNESS);
  }

  @Test
  void readsNativePotionContentsFromItem() {
    ItemStack stack = new ItemStack(Items.BUCKET);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));

    assertThat(PotionFluidType.getPotionContents(stack).potion()).contains(Potions.REGENERATION);
  }
}

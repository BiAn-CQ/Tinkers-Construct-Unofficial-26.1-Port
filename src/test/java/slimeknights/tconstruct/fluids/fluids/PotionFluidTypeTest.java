package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

class PotionFluidTypeTest extends BaseMcTest {
  @Test
  void readsLegacyPotionIdFromCustomData() {
    FluidStack stack = new FluidStack(Fluids.WATER, 50);
    CompoundTag legacy = new CompoundTag();
    legacy.putString("Potion", "minecraft:swiftness");
    legacy.putInt("CustomPotionColor", 0x123456);
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));

    assertThat(PotionFluidType.getPotionContents(stack).potion()).contains(Potions.SWIFTNESS);
    assertThat(PotionFluidType.getPotionContents(stack).customColor()).contains(0x123456);
  }

  @Test
  void nativePotionComponentTakesPriorityOverLegacyData() {
    FluidStack stack = new FluidStack(Fluids.WATER, 50);
    CompoundTag legacy = new CompoundTag();
    legacy.putString("Potion", "minecraft:swiftness");
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));

    assertThat(PotionFluidType.getPotionContents(stack).potion()).contains(Potions.STRENGTH);
  }

  @Test
  void readsLegacyPotionDataFromModItem() {
    ItemStack stack = new ItemStack(Items.BUCKET);
    CompoundTag legacy = new CompoundTag();
    legacy.putString("Potion", "minecraft:regeneration");
    legacy.putInt("CustomPotionColor", 0x654321);
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));

    assertThat(PotionFluidType.getPotionContents(stack).potion()).contains(Potions.REGENERATION);
    assertThat(PotionFluidType.getPotionContents(stack).customColor()).contains(0x654321);
  }
}

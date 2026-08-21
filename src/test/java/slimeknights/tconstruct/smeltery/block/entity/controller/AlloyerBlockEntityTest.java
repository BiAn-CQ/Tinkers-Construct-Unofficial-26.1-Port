package slimeknights.tconstruct.smeltery.block.entity.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.model.data.ModelData;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.client.model.ModelProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlloyerBlockEntityTest {
  @Test
  void exposesTankContentsToNativeBlockModel() {
    BlockEntityType<?> type = mock(BlockEntityType.class);
    when(type.isValid(any())).thenReturn(true);
    AlloyerBlockEntity alloyer = new AlloyerBlockEntity(type, BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    ModelData data = alloyer.getModelData();

    assertThat(data.get(ModelProperties.FLUID_STACK)).isSameAs(FluidStack.EMPTY);
    assertThat(data.get(ModelProperties.TANK_CAPACITY)).isEqualTo(alloyer.getTank().getCapacity());
  }
}

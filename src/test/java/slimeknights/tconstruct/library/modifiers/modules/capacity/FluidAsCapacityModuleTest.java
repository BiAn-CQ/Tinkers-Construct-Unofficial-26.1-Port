package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FluidAsCapacityModuleTest extends BaseMcTest {
  @Test
  void addingCapacityFillsAnEmptyTank() {
    ToolTankHelper helper = mock(ToolTankHelper.class);
    IToolStackView tool = mock(IToolStackView.class);
    when(helper.getFluid(tool)).thenReturn(FluidStack.EMPTY);
    new FluidAsCapacityModule(helper, Fluids.WATER).addAmount(tool, null, 100);
    ArgumentCaptor<FluidStack> fluid = ArgumentCaptor.forClass(FluidStack.class);
    verify(helper).setFluid(eq(tool), fluid.capture());
    assertThat(fluid.getValue().getFluid()).isSameAs(Fluids.WATER);
    assertThat(fluid.getValue().getAmount()).isEqualTo(100);
  }

  @Test
  void addingCapacityDoesNotReplaceAnotherFluidOrWriteForZero() {
    ToolTankHelper helper = mock(ToolTankHelper.class);
    IToolStackView tool = mock(IToolStackView.class);
    when(helper.getFluid(tool)).thenReturn(new FluidStack(Fluids.LAVA, 50));
    FluidAsCapacityModule module = new FluidAsCapacityModule(helper, Fluids.WATER);
    module.addAmount(tool, null, 100);
    verify(helper, never()).setFluid(any(), any());
    clearInvocations(helper);
    module.addAmount(tool, null, 0);
    module.removeAmount(tool, null, 0);
    verifyNoInteractions(helper);
  }
}

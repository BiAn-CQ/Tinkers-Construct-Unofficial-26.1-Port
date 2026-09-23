package slimeknights.tconstruct.library.tools.capability.fluid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerTools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ToolFluidCapabilityTest extends BaseMcTest {
  @Test
  void transferReportsOnlyTheContainersActuallyExchanged() {
    ItemStack stack = new ItemStack(TinkerTools.pickaxe.get());
    ItemStackDataUtil.updateTagElement(stack, ToolStack.TAG_VOLATILE_MOD_DATA,
      tag -> tag.putInt(ToolFluidCapability.TOTAL_TANKS.toString(), 1));
    ItemAccess access = mock(ItemAccess.class);
    when(access.getResource()).thenReturn(ItemResource.of(stack));
    when(access.getAmount()).thenReturn(2);
    var hook = mock(ToolFluidCapability.FluidModifierHook.class);
    when(hook.insert(any(), any(), eq(0), any())).thenReturn(100);
    FluidResource water = FluidResource.of(Fluids.WATER);
    when(hook.extract(any(), any(), eq(0), any())).thenReturn(water.toStack(100));
    var handler = new ToolFluidCapability(access) {
      @Override
      protected FluidModifierHook findHook(IToolStackView tool, int tank) {
        return hook;
      }
    };
    for (int exchanged : new int[]{0, 1, 2}) {
      try (Transaction transaction = Transaction.openRoot()) {
        when(access.exchange(any(), eq(2), same(transaction))).thenReturn(exchanged);
        assertThat(handler.insert(0, water, 200, transaction)).isEqualTo(exchanged * 100);
        assertThat(handler.extract(0, water, 200, transaction)).isEqualTo(exchanged * 100);
      }
    }
  }
}

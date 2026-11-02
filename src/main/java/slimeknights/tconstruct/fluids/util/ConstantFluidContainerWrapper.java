package slimeknights.tconstruct.fluids.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/** @deprecated use the shared native container handler. */
@Deprecated(forRemoval = true)
public class ConstantFluidContainerWrapper extends slimeknights.mantle.item.ConstantFluidHandlerItem {
  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess access) { super(fluid, access); }
  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess access, @Nullable ItemResource empty) { super(fluid, access, empty); }
}

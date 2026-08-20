package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Fluid handler wrapper that only allows filling
 */
public class FillOnlyFluidHandler implements IndexedFluidHandler {
	private final IFluidHandler parent;
	public FillOnlyFluidHandler(IFluidHandler parent) {
		this.parent = parent;
	}

	@Override
	public int getTanks() {
		return parent.getTanks();
	}

	@Nonnull
	@Override
	public FluidStack getFluidInTank(int tank) {
		return parent.getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return parent.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return false;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return parent.fill(resource, action);
	}

	@Override
	public int fill(int tank, FluidStack resource, FluidAction action) {
		if (parent instanceof IndexedFluidHandler indexed) {
			return indexed.fill(tank, resource, action);
		}
		return tank == 0 && parent.getTanks() == 1 ? parent.fill(resource, action) : 0;
	}

	@Nonnull
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Nonnull
	@Override
	public FluidStack drain(int tank, FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Nonnull
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}
}

package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Extension of the legacy fluid handler API that can mutate one explicit tank.
 *
 * <p>The legacy {@link IFluidHandler} mutation methods select a tank internally.
 * That is insufficient when adapting the handler to NeoForge's indexed transfer
 * API, as a simulated operation against tank N must commit against the same
 * tank. Multi-tank handlers exposed through that adapter should implement this
 * interface instead of relying on global fill/drain selection.</p>
 */
public interface IndexedFluidHandler extends IFluidHandler {
  /** Inserts into one explicit tank. */
  int fill(int tank, FluidStack resource, FluidAction action);

  /** Extracts the requested resource from one explicit tank. */
  FluidStack drain(int tank, FluidStack resource, FluidAction action);
}

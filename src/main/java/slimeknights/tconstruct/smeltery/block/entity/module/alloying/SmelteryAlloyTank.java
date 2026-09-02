package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

/**
 * Smeltery implementation of the alloy tank, basically just a wrapper around the smeltery tank
 */
@RequiredArgsConstructor
public class SmelteryAlloyTank implements IMutableAlloyTank {
  /**
   * Handler parent
   */
  private final SmelteryTank<?> handler;
  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter @Setter
  private int temperature = 0;

  @Override
  public int getTanks() {
    return handler.size();
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    return tank >= 0 && tank < handler.getFluids().size() ? handler.getFluids().get(tank) : FluidStack.EMPTY;
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    // the fluid fits if the net gain in fluid fits in the empty space
    return (fluid.getAmount() - removed) <= handler.getRemainingSpace();
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    if (fluidStack.isEmpty() || tank < 0 || tank >= handler.getFluids().size()) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = FluidResource.of(fluidStack);
    try (Transaction transaction = Transaction.open(null)) {
      int drained = handler.extract(tank, resource, fluidStack.getAmount(), transaction);
      if (drained > 0) {
        transaction.commit();
        return resource.toStack(drained);
      }
    }
    return FluidStack.EMPTY;
  }

  @Override
  public int fill(FluidStack fluidStack) {
    if (fluidStack.isEmpty()) {
      return 0;
    }
    try (Transaction transaction = Transaction.open(null)) {
      int filled = handler.insert(FluidResource.of(fluidStack), fluidStack.getAmount(), transaction);
      if (filled > 0) {
        transaction.commit();
      }
      return filled;
    }
  }
}

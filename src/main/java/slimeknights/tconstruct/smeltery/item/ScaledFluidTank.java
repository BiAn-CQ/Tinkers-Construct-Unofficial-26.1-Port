package slimeknights.tconstruct.smeltery.item;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Predicate;

/**
 * Native fluid handler representing a stack of tanks. Every operation affects all tanks in the
 * item stack together, so amounts are truncated to a multiple of {@code scale}.
 */
public class ScaledFluidTank extends FluidStacksResourceHandler {
  private final int scale;
  private Predicate<FluidStack> validator = fluid -> true;

  private ScaledFluidTank(int capacity, int scale) {
    super(1, capacity * scale);
    this.scale = scale;
  }

  /** Creates a new instance */
  public static ScaledFluidTank create(int capacity, int scale) {
    return new ScaledFluidTank(capacity, scale);
  }

  /** Updates the total capacity. */
  public ScaledFluidTank setCapacity(int capacity) {
    this.capacity = enforceScale(capacity);
    return this;
  }

  /** Limits resources accepted by this tank. */
  public ScaledFluidTank setValidator(Predicate<FluidStack> validator) {
    this.validator = validator;
    return this;
  }

  /** Directly replaces the stored fluid. */
  public void setFluid(FluidStack stack) {
    FluidStack scaled = enforceScale(stack, true);
    set(0, FluidResource.of(scaled), scaled.getAmount());
  }

  /** Gets a copy of the stored fluid. */
  public FluidStack getFluid() {
    return FluidUtil.getStack(this, 0);
  }

  public int getFluidAmount() {
    return getAmountAsInt(0);
  }

  public int getCapacity() {
    return getCapacityAsInt(0, getResource(0));
  }

  public boolean isEmpty() {
    return getResource(0).isEmpty();
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return validator.test(resource.toStack(1));
  }

  /** enforces the amount matches the scale */
  private int enforceScale(int amount) {
    // no working with fluids of partial amounts
    int remainder = amount % scale;
    if (remainder != 0) {
      amount -= remainder;
    }
    return amount;
  }

  /** enforces the fluid matches the scale */
  private FluidStack enforceScale(FluidStack stack, boolean copy) {
    // no working with fluids of partial amounts
    int remainder = stack.getAmount() % scale;
    if (remainder != 0) {
      if (copy) {
        stack = stack.copy();
      }
      stack.shrink(remainder);
    }
    return stack;
  }


  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return super.insert(index, resource, enforceScale(amount), transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return super.extract(index, resource, enforceScale(amount), transaction);
  }
}

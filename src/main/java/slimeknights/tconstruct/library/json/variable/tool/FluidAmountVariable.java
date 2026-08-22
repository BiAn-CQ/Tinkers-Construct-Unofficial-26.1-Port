package slimeknights.tconstruct.library.json.variable.tool;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Returns the amount in a tool tank when its fluid matches the configured predicate. */
public record FluidAmountVariable(ToolTankHelper helper, IJsonPredicate<Fluid> fluid) implements ToolVariable {
  public static final RecordLoadable<FluidAmountVariable> LOADER = RecordLoadable.create(
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, false, FluidAmountVariable::helper),
    FluidPredicate.LOADER.defaultField("fluid", FluidAmountVariable::fluid),
    FluidAmountVariable::new);

  public FluidAmountVariable(IJsonPredicate<Fluid> fluid) { this(ToolTankHelper.TANK_HELPER, fluid); }

  @Override public float getValue(IToolStackView tool) {
    FluidStack stored = helper.getFluid(tool);
    return !stored.isEmpty() && fluid.matches(stored.getFluid()) ? stored.getAmount() : 0;
  }

  @Override public RecordLoadable<FluidAmountVariable> getLoader() { return LOADER; }
}

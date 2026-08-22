package slimeknights.tconstruct.library.json.variable.tool;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Returns tool tank capacity when the current fluid matches the configured predicate. */
public record TankCapacityVariable(ToolTankHelper helper, IJsonPredicate<Fluid> fluid) implements ToolVariable {
  public static final RecordLoadable<TankCapacityVariable> LOADER = RecordLoadable.create(
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, false, TankCapacityVariable::helper),
    FluidPredicate.LOADER.defaultField("fluid", TankCapacityVariable::fluid),
    TankCapacityVariable::new);

  public TankCapacityVariable() { this(ToolTankHelper.TANK_HELPER, FluidPredicate.ANY); }
  public TankCapacityVariable(ToolTankHelper helper) { this(helper, FluidPredicate.ANY); }
  public TankCapacityVariable(IJsonPredicate<Fluid> fluid) { this(ToolTankHelper.TANK_HELPER, fluid); }

  @Override public float getValue(IToolStackView tool) {
    FluidStack stored = helper.getFluid(tool);
    return stored.isEmpty() || fluid.matches(stored.getFluid()) ? helper.getCapacity(tool) : 0;
  }

  @Override public RecordLoadable<TankCapacityVariable> getLoader() { return LOADER; }
}

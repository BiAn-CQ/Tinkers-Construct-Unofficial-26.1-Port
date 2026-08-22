package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.special.CapacityBarHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Exposes one exact fluid in a tool tank as a modifier capacity bar. */
public record FluidAsCapacityModule(ToolTankHelper helper, Fluid fluid) implements ModifierModule, CapacityBarHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<FluidAsCapacityModule>defaultHooks(ModifierHooks.CAPACITY_BAR);
  public static final RecordLoadable<FluidAsCapacityModule> LOADER = RecordLoadable.create(
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, FluidAsCapacityModule::helper),
    Loadables.FLUID.requiredField("fluid", FluidAsCapacityModule::fluid),
    FluidAsCapacityModule::new);

  public FluidAsCapacityModule(Fluid fluid) { this(ToolTankHelper.TANK_HELPER, fluid); }
  @Override public RecordLoadable<FluidAsCapacityModule> getLoader() { return LOADER; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }

  @Override public int getAmount(IToolStackView tool) {
    FluidStack stored = helper.getFluid(tool);
    return !stored.isEmpty() && stored.getFluid() == fluid ? stored.getAmount() : 0;
  }

  @Override public int getCapacity(IToolStackView tool, ModifierEntry entry) {
    FluidStack stored = helper.getFluid(tool);
    return stored.isEmpty() || stored.getFluid() == fluid ? helper.getCapacity(tool) : 0;
  }

  @Override public void setAmount(IToolStackView tool, ModifierEntry entry, int amount) {
    FluidStack stored = helper.getFluid(tool);
    if (stored.isEmpty()) {
      if (amount > 0) helper.setFluid(tool, new FluidStack(fluid, amount));
    } else if (stored.getFluid() == fluid) {
      helper.setFluid(tool, stored.copyWithAmount(amount));
    }
  }
}

package slimeknights.tconstruct.library.tools.capability.fluid;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.CompoundIndexHookIterator;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability.FluidModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Shared logic to iterate fluid capabilities for {@link ToolFluidCapability}
 */
abstract class FluidModifierHookIterator<I> extends CompoundIndexHookIterator<FluidModifierHook,I> {
  /** Entry from {@link #findHook(IToolStackView, int)}, will be set during or before iteration */
  protected ModifierEntry indexEntry = null;

  @Override
  protected int getSize(IToolStackView tool, FluidModifierHook hook) {
    return hook.getTanks(tool.getVolatileData(), indexEntry);
  }
}

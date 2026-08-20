package slimeknights.tconstruct.library.client.model;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.model.data.ModelProperty;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

/** Server-safe stub for block model data properties. */
public final class ModelProperties {
  private ModelProperties() {}

  public static final ModelProperty<MaterialVariantId> MATERIAL = new ModelProperty<>();
  public static final ModelProperty<FluidStack> FLUID_STACK = new ModelProperty<>();
  public static final ModelProperty<Integer> TANK_CAPACITY = new ModelProperty<>();
}

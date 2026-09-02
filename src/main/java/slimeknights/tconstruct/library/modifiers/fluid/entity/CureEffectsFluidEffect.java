package slimeknights.tconstruct.library.modifiers.fluid.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.utils.SimulationMode;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;
import slimeknights.tconstruct.library.utils.EffectCureUtil;

/**
 * Effect to clear all effects using the given stack
 * @param stack  Stack used for curing, standard is milk bucket
 */
public record CureEffectsFluidEffect(ItemStackTemplate stack) implements FluidEffect<FluidEffectContext.Entity> {
  public static final RecordLoadable<CureEffectsFluidEffect> LOADER = RecordLoadable.create(ItemStackTemplateLoadable.STACK.requiredField("item", CureEffectsFluidEffect::stack), CureEffectsFluidEffect::new);

  public CureEffectsFluidEffect(ItemLike item) {
    this(new ItemStackTemplate(item.asItem()));
  }

  /** Creates an effect from an already constructed stack for source compatibility. */
  public CureEffectsFluidEffect(ItemStack stack) {
    this(ItemStackTemplate.fromNonEmptyStack(stack));
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, Entity context, SimulationMode action) {
    ItemStack stack = this.stack.create();
    LivingEntity target = context.getLivingTarget();
    if (target != null && level.isFull()) {
      // when simulating, search the effects list directly for curative effects
      // may still be wrong if the event cancels things though, no way to safely simulate it
      if (action.simulate()) {
        return target.getActiveEffects().stream().anyMatch(effect -> EffectCureUtil.canCure(effect, stack)) ? 1 : 0;
      }
      return EffectCureUtil.removeEffectsCuredBy(target, stack) ? 1 : 0;
    }
    return 0;
  }

  @Override
  public RecordLoadable<CureEffectsFluidEffect> getLoader() {
    return LOADER;
  }
}

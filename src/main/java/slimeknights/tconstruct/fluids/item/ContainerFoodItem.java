package slimeknights.tconstruct.fluids.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** @deprecated use the shared Mantle container food. */
@Deprecated(forRemoval = true)
public class ContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem {
  public ContainerFoodItem(Properties props) {
    super(props);
  }

  /** Adds effects to the tooltip */
  public static void addEffectTooltip(Iterable<MobEffectInstance> effects, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (MobEffectInstance effect : effects) {
      MutableComponent mutable = Component.translatable(effect.getDescriptionId());
      if (effect.getAmplifier() > 0) {
        mutable = Component.translatable("potion.withAmplifier", mutable, Component.translatable("potion.potency." + effect.getAmplifier()));
      }
      if (effect.getDuration() > 20) {
        mutable = Component.translatable("potion.withDuration", mutable, MobEffectUtil.formatDuration(effect, 1.0f, 20.0f));
      }
      tooltip.add(mutable.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
    }
  }

  /** Adds status effects stored in the 26.1 consumable component. */
  public static void addEffectTooltip(Consumable consumable, Consumer<Component> tooltip) {
    if (consumable == null) {
      return;
    }
    for (var effect : consumable.onConsumeEffects()) {
      if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
        List<Component> lines = new java.util.ArrayList<>();
        addEffectTooltip(apply.effects(), lines);
        lines.forEach(tooltip);
      }
    }
  }

  /** @deprecated use Mantle's fluid container food. */
  @Deprecated(forRemoval = true)
  public static class FluidContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem {
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) { super(props, fluid); }
    public ResourceHandler<FluidResource> getFluidHandler(ItemAccess access) { return createFluidHandler(access); }
  }
}

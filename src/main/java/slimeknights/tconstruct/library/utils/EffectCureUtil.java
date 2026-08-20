package slimeknights.tconstruct.library.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.tools.modifiers.effect.NoMilkEffect;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Small compatibility layer for the per-instance effect cure API removed in 26.1.
 * Cure metadata is kept on the live effect instance, which preserves the old
 * item-sensitive behavior without reintroducing the removed NeoForge classes.
 */
public final class EffectCureUtil {
  private static final Map<MobEffectInstance, Set<Item>> CURES = Collections.synchronizedMap(new IdentityHashMap<>());

  private EffectCureUtil() {}

  /** Clears all explicit cures for the given instance. */
  public static void clearCures(MobEffectInstance effect) {
    CURES.put(effect, Collections.emptySet());
  }

  /** Sets the explicit item cures for the given instance. */
  public static void setCures(MobEffectInstance effect, Collection<Item> items) {
    CURES.put(effect, Set.copyOf(items));
  }

  /** Returns true if this effect has a cure path in the compatibility layer. */
  public static boolean hasCures(MobEffectInstance effect) {
    Set<Item> explicit = CURES.get(effect);
    return explicit == null ? !(effect.getEffect().value() instanceof NoMilkEffect) : !explicit.isEmpty();
  }

  /** Checks whether an item can cure this particular effect instance. */
  public static boolean canCure(MobEffectInstance effect, ItemStack stack) {
    Set<Item> explicit = CURES.get(effect);
    if (explicit != null) {
      return explicit.contains(stack.getItem());
    }
    if (stack.is(Items.MILK_BUCKET)) {
      return !(effect.getEffect().value() instanceof NoMilkEffect);
    }
    return stack.is(Items.HONEY_BOTTLE) && effect.getEffect() == MobEffects.POISON;
  }

  /** Removes all effects that can be cured by the given stack. */
  public static boolean removeEffectsCuredBy(LivingEntity entity, ItemStack stack) {
    boolean removed = false;
    for (MobEffectInstance effect : entity.getActiveEffects().toArray(MobEffectInstance[]::new)) {
      if (canCure(effect, stack)) {
        removed |= entity.removeEffect(effect.getEffect());
      }
    }
    return removed;
  }
}

package slimeknights.tconstruct.shared;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.List;

/** Food definitions shared by item components and the custom cake blocks. */
@SuppressWarnings("WeakerAccess")
public final class TinkerFood {
  private TinkerFood() {}

  /** 26.1 split food stats from the consume effects into separate components. */
  public record Entry(FoodProperties food, Consumable consumable, List<MobEffectInstance> effects) {
    public Entry {
      effects = List.copyOf(effects);
    }
  }

  private static Entry create(int nutrition, float saturation, boolean alwaysEat, ItemUseAnimation animation,
                              float consumeSeconds, MobEffectInstance... effects) {
    FoodProperties food = new FoodProperties(nutrition, saturation, alwaysEat);
    Consumable.Builder consumable = (animation == ItemUseAnimation.DRINK ? Consumables.defaultDrink() : Consumables.defaultFood())
      .consumeSeconds(consumeSeconds);
    if (effects.length > 0) {
      consumable.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects)));
    }
    return new Entry(food, consumable.build(), List.of(effects));
  }

  private static Entry food(int nutrition, float saturation, boolean alwaysEat, MobEffectInstance... effects) {
    return create(nutrition, saturation, alwaysEat, ItemUseAnimation.EAT, 1.6F, effects);
  }

  private static Entry drink(boolean alwaysEat, MobEffectInstance... effects) {
    return create(0, 0, alwaysEat, ItemUseAnimation.DRINK, 1.6F, effects);
  }

  private static Entry fastFood(int nutrition, float saturation, boolean alwaysEat, MobEffectInstance... effects) {
    return create(nutrition, saturation, alwaysEat, ItemUseAnimation.EAT, 0.8F, effects);
  }

  /** Bacon. What more is there to say? */
  public static final Entry BACON = food(4, 0.6F, false);

  /** Cheese is used for both the block and the ingot, eating the block returns 3 ingots. */
  public static final Entry CHEESE = food(3, 0.4F, false);

  /** For the modifier. */
  public static final Entry JEWELED_APPLE = food(4, 1.2F, true,
    new MobEffectInstance(MobEffects.HASTE, 1200, 0),
    new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0));

  /* Cake blocks are set up to take food as a parameter. */
  public static final Entry EARTH_CAKE = food(1, 0.3F, true, new MobEffectInstance(TinkerEffects.bouncy, 30 * 20, 0));
  public static final Entry SKY_CAKE = food(1, 0.2F, true, new MobEffectInstance(TinkerEffects.doubleJump, 30 * 20, 0));
  public static final Entry ICHOR_CAKE = food(1, 0.3F, true, new MobEffectInstance(TinkerEffects.antigravity, 30 * 20, 0));
  public static final Entry ENDER_CAKE = fastFood(1, 0.4F, true, new MobEffectInstance(TinkerEffects.returning, 30 * 20, 0));
  public static final Entry MAGMA_CAKE = food(2, 0.2F, true, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30 * 20, 0));
  // Regen is 50 ticks per half heart, so this heals 3 per slice.
  public static final Entry BLOOD_CAKE = food(2, 0.2F, true, new MobEffectInstance(MobEffects.REGENERATION, 3 * 50, 0));

  public static final Entry EARTH_BOTTLE = drink(true,
    new MobEffectInstance(TinkerEffects.experienced, 120 * 20),
    new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1));
  public static final Entry SKY_BOTTLE = drink(true,
    new MobEffectInstance(TinkerEffects.ricochet, 120 * 20),
    new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1));
  public static final Entry ICHOR_BOTTLE = drink(true,
    new MobEffectInstance(MobEffects.LEVITATION, 10 * 20),
    new MobEffectInstance(MobEffects.SLOWNESS, 10 * 20, 1));
  public static final Entry ENDER_BOTTLE = drink(true,
    new MobEffectInstance(TinkerEffects.enderference, 60 * 20),
    new MobEffectInstance(MobEffects.SLOWNESS, 60 * 20, 1));
  public static final Entry VENOM_BOTTLE = drink(true,
    new MobEffectInstance(MobEffects.STRENGTH, 30 * 20),
    new MobEffectInstance(MobEffects.POISON, 250));

  /** @deprecated no longer used. */
  @Deprecated(forRemoval = true)
  public static final Entry MAGMA_BOTTLE = drink(true);

  /** Soup keeps its legacy food value while using the 26.1 drink consumable. */
  public static final Entry MEAT_SOUP = create(8, 0.6F, false, ItemUseAnimation.DRINK, 1.6F);

  /** Gets the cake for the given foliage type. */
  public static Entry getCake(FoliageType slime) {
    return switch (slime) {
      default -> EARTH_CAKE;
      case SKY -> SKY_CAKE;
      case ICHOR -> ICHOR_CAKE;
      case BLOOD -> BLOOD_CAKE;
      case ENDER -> ENDER_CAKE;
    };
  }

  /** Gets the bottle food for the given slime type. */
  public static Entry getBottle(SlimeType slime) {
    return switch (slime) {
      default -> EARTH_BOTTLE;
      case SKY -> SKY_BOTTLE;
      case ICHOR -> ICHOR_BOTTLE;
      case ENDER -> ENDER_BOTTLE;
    };
  }
}

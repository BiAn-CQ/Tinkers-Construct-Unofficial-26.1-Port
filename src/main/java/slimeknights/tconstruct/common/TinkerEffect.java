package slimeknights.tconstruct.common;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Effect extension with a few helpers */
public class TinkerEffect extends MobEffect {
  /** If true, effect is visible, false for hidden */
  private final boolean show;
  public TinkerEffect(MobEffectCategory typeIn, boolean show) {
    this(typeIn, 0xffffff, show);
  }

  public TinkerEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color);
    this.show = show;
  }

  // override to change return type
  @Override
  public TinkerEffect addAttributeModifier(Holder<Attribute> attribute, Identifier id, double amount, Operation operation) {
    super.addAttributeModifier(attribute, id, amount, operation);
    return this;
  }

  /** Compatibility overload for the pre-1.21 UUID string contract. */
  @Deprecated(forRemoval = true)
  public TinkerEffect addAttributeModifier(Holder<Attribute> attribute, String uuid, double amount, Operation operation) {
    return addAttributeModifier(attribute, Identifier.fromNamespaceAndPath("tconstruct", uuid), amount, operation);
  }

  /** Compatibility overload for the pre-1.21 value/UUID contract. */
  @Deprecated(forRemoval = true)
  public TinkerEffect addAttributeModifier(Attribute attribute, String uuid, double amount, Operation operation) {
    return addAttributeModifier(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), Identifier.fromNamespaceAndPath("tconstruct", uuid), amount, operation);
  }

  /* Visibility */

  public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
    consumer.accept(new IClientMobEffectExtensions() {
      @Override
      public boolean isVisibleInInventory(MobEffectInstance effect) {
        return show;
      }

      @Override
      public boolean isVisibleInGui(MobEffectInstance effect) {
        return show;
      }
    });
  }

  /* Helpers */

  /**
   * Applies this potion to an entity
   * @param entity    Entity
   * @param duration  Duration
   * @return  Applied instance
   * @deprecated use {@link LivingEntity#addEffect(MobEffectInstance)}
   */
  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration) {
    return this.apply(entity, duration, 0);
  }

  /**
   * Applies this potion to an entity
   * @param entity    Entity
   * @param duration  Duration
   * @param level     Effect level
   * @return  Applied instance
   * @deprecated use {@link LivingEntity#addEffect(MobEffectInstance)}
   */
  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration, int level) {
    return this.apply(entity, duration, level, false);
  }

  /**
   * Applies this potion to an entity
   * @param entity    Entity
   * @param duration  Duration
   * @param amplifier Effect level
   * @param showIcon  If true, shows an icon in the HUD
   * @return  Applied instance
   * @deprecated use {@link LivingEntity#addEffect(MobEffectInstance)}
   */
  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration, int amplifier, boolean showIcon) {
    MobEffectInstance effect = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this), duration, amplifier, false, false, showIcon);
    entity.addEffect(effect);
    return effect;
  }

  /**
   * Gets the level of the effect on the entity starting from 1, or 0 if not active
   * @param entity  Entity to check
   * @return  Level, or 0 if inactive
   */
  public static int getLevel(LivingEntity entity, MobEffect effect) {
    return getAmplifier(entity, effect) + 1;
  }

  public static int getLevel(LivingEntity entity, Holder<MobEffect> effect) {
    return getLevel(entity, effect.value());
  }

  /**
   * Gets the level of the effect on the entity starting from 1, or 0 if not active
   * @param entity  Entity to check
   * @return  Level, or 0 if inactive
   */
  public static int getLevel(LivingEntity entity, Supplier<? extends MobEffect> effect) {
    return getAmplifier(entity, effect.get()) + 1;
  }

  /**
   * Gets the amplifier of the effect on the entity starting from 0, or -1 if not active
   * @param entity  Entity to check
   * @return  Amplifier, or -1 if inactive
   */
  public static int getAmplifier(LivingEntity entity, MobEffect effect) {
    MobEffectInstance instance = entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
    if (instance != null) {
      return instance.getAmplifier();
    }
    return -1;
  }

  /** @deprecated use {@link #getAmplifier(LivingEntity, MobEffect)} which is better named or {@link #getLevel(LivingEntity, MobEffect)} which gives a more useful return */
  @Deprecated(forRemoval = true)
  public int getLevel(LivingEntity entity) {
    return getAmplifier(entity, this);
  }
}

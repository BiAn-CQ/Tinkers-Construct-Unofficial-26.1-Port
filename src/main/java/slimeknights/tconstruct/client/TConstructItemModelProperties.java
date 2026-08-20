package slimeknights.tconstruct.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import javax.annotation.Nullable;

/**
 * Native 26.1 item model properties replacing the removed 1.20.1
 * {@code ItemProperties.register} table.
 *
 * <p>The values intentionally mirror the old Tinkers properties.  Keeping
 * the state calculation in one place is important: the item-definition
 * generator can use normal 26.1 condition/range/select model nodes while
 * custom tools and third-party tool definitions can still register the same
 * property identifiers.</p>
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructItemModelProperties {
  private static final Identifier BROKEN_ID = TConstruct.getResource("broken");
  private static final Identifier CHARGING_ID = TConstruct.getResource("charging");
  private static final Identifier CHARGE_ID = TConstruct.getResource("charge");
  private static final Identifier CAST_ID = TConstruct.getResource("cast");
  private static final Identifier AMMO_ID = TConstruct.getResource("ammo");

  private TConstructItemModelProperties() {}

  /**
   * Returns the numeric form used by the compact legacy override adapter.
   * Numeric predicates use the same greater-than-or-equal comparison as the
   * removed 1.20.1 item-property resolver.
   */
  public static float getValue(String property, ItemStack stack, @Nullable ClientLevel level,
                               @Nullable ItemOwner owner, ItemDisplayContext displayContext, int seed) {
    LivingEntity entity = owner == null ? null : owner.asLivingEntity();
    return switch (property) {
      case "tconstruct:broken" -> ToolDamageUtil.isBroken(stack) ? 1.0f : 0.0f;
      case "tconstruct:charging" -> charging(stack, entity);
      case "tconstruct:charge" -> charge(stack, entity);
      case "tconstruct:cast" -> cast(stack, entity) ? 1.0f : 0.0f;
      case "tconstruct:ammo" -> switch (ammo(stack)) {
        case ARROW -> 1.0f;
        case ROCKET -> 2.0f;
        case NONE -> 0.0f;
      };
      default -> 0.0f;
    };
  }

  @SubscribeEvent
  static void registerConditional(RegisterConditionalItemModelPropertyEvent event) {
    event.register(BROKEN_ID, Broken.MAP_CODEC);
    event.register(CHARGING_ID, ChargingCondition.MAP_CODEC);
    event.register(CAST_ID, Cast.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerRange(RegisterRangeSelectItemModelPropertyEvent event) {
    event.register(CHARGING_ID, Charging.MAP_CODEC);
    event.register(CHARGE_ID, Charge.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerSelect(RegisterSelectItemModelPropertyEvent event) {
    event.register(AMMO_ID, Ammo.TYPE);
  }

  /** True when the tool's fake durability has reached its broken state. */
  public record Broken() implements ConditionalItemModelProperty {
    public static final MapCodec<Broken> MAP_CODEC = MapCodec.unit(new Broken());

    @Override
    public boolean get(ItemStack stack, ClientLevel level, @Nullable LivingEntity entity, int seed,
                       ItemDisplayContext displayContext) {
      return ToolDamageUtil.isBroken(stack);
    }

    @Override
    public MapCodec<Broken> type() {
      return MAP_CODEC;
    }
  }

  /** Compatibility condition for integrations that only need a pull test. */
  public record ChargingCondition() implements ConditionalItemModelProperty {
    public static final MapCodec<ChargingCondition> MAP_CODEC = MapCodec.unit(new ChargingCondition());

    @Override
    public boolean get(ItemStack stack, ClientLevel level, @Nullable LivingEntity entity, int seed,
                       ItemDisplayContext displayContext) {
      return charging(stack, entity) > 0;
    }

    @Override
    public MapCodec<ChargingCondition> type() {
      return MAP_CODEC;
    }
  }

  /** Numeric pull/block state used by the old {@code tconstruct:charging} predicate. */
  public record Charging() implements RangeSelectItemModelProperty {
    public static final MapCodec<Charging> MAP_CODEC = MapCodec.unit(new Charging());

    @Override
    public float get(ItemStack stack, ClientLevel level, @Nullable ItemOwner owner, int seed) {
      return charging(stack, owner == null ? null : owner.asLivingEntity());
    }

    @Override
    public MapCodec<Charging> type() {
      return MAP_CODEC;
    }
  }

  /** Numeric draw progress used by the old {@code tconstruct:charge} predicate. */
  public record Charge() implements RangeSelectItemModelProperty {
    public static final MapCodec<Charge> MAP_CODEC = MapCodec.unit(new Charge());

    @Override
    public float get(ItemStack stack, ClientLevel level, @Nullable ItemOwner owner, int seed) {
      return charge(stack, owner == null ? null : owner.asLivingEntity());
    }

    @Override
    public MapCodec<Charge> type() {
      return MAP_CODEC;
    }
  }

  /** True while a Tinkers fishing rod has an active fishing hook. */
  public record Cast() implements ConditionalItemModelProperty {
    public static final MapCodec<Cast> MAP_CODEC = MapCodec.unit(new Cast());

    @Override
    public boolean get(ItemStack stack, ClientLevel level, @Nullable LivingEntity entity, int seed,
                       ItemDisplayContext displayContext) {
      return cast(stack, entity);
    }

    @Override
    public MapCodec<Cast> type() {
      return MAP_CODEC;
    }
  }

  /** Select value for the custom crossbow's persistent ammo payload. */
  public record Ammo() implements SelectItemModelProperty<CrossbowItem.ChargeType> {
    public static final MapCodec<Ammo> MAP_CODEC = MapCodec.unit(new Ammo());
    public static final Type<Ammo, CrossbowItem.ChargeType> TYPE = Type.create(MAP_CODEC, CrossbowItem.ChargeType.CODEC);

    @Override
    public CrossbowItem.ChargeType get(ItemStack stack, ClientLevel level, @Nullable LivingEntity entity, int seed,
                                       ItemDisplayContext displayContext) {
      return ammo(stack);
    }

    @Override
    public Codec<CrossbowItem.ChargeType> valueCodec() {
      return CrossbowItem.ChargeType.CODEC;
    }

    @Override
    public Type<Ammo, CrossbowItem.ChargeType> type() {
      return TYPE;
    }
  }

  private static float charging(ItemStack stack, @Nullable LivingEntity holder) {
    if (holder != null && holder.isUsingItem() && matches(holder.getUseItem(), stack)) {
      ItemUseAnimation animation = stack.getUseAnimation();
      if (animation == ItemUseAnimation.BLOCK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 2.5f : 2.0f;
      }
      if (animation == ItemUseAnimation.TRIDENT) {
        return 1.75f;
      }
      if (animation != ItemUseAnimation.EAT && animation != ItemUseAnimation.DRINK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 1.5f : 1.0f;
      }
    }
    return 0.0f;
  }

  private static float charge(ItemStack stack, @Nullable LivingEntity holder) {
    if (holder == null || !matches(holder.getUseItem(), stack)) {
      return 0.0f;
    }
    int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
    return drawtime == -1 ? 0.0f
      : (float) (stack.getUseDuration(holder) - holder.getUseItemRemainingTicks()) / drawtime;
  }

  private static boolean cast(ItemStack stack, @Nullable LivingEntity entity) {
    if (!(entity instanceof Player player) || player.fishing == null
        || !stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
      return false;
    }
    ItemStack mainhand = player.getMainHandItem();
    return matches(mainhand, stack) || matches(player.getOffhandItem(), stack)
      && !mainhand.canPerformAction(ItemAbilities.FISHING_ROD_CAST);
  }

  /**
   * Model property callbacks are allowed to receive a component-identical view of the rendered stack
   * instead of the same Java object held by the entity. Identity checks therefore make active-use
   * states flicker between frames on tools whose model is resolved from a copied stack.
   */
  private static boolean matches(ItemStack first, ItemStack second) {
    return first == second || ItemStack.isSameItemSameComponents(first, second);
  }

  private static CrossbowItem.ChargeType ammo(ItemStack stack) {
    CompoundTag nbt = ItemStackDataUtil.getTag(stack);
    if (nbt != null) {
      CompoundTag persistentData = nbt.getCompoundOrEmpty(ToolStack.TAG_PERSISTENT_MOD_DATA);
      if (!persistentData.isEmpty()) {
        CompoundTag ammo = persistentData.getCompoundOrEmpty(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO.toString());
        if (!ammo.isEmpty()) {
          String id = ammo.getStringOr("id", "");
          return id.equals("minecraft:firework_rocket")
            ? CrossbowItem.ChargeType.ROCKET : CrossbowItem.ChargeType.ARROW;
        }
      }
    }
    return CrossbowItem.ChargeType.NONE;
  }
}

package slimeknights.tconstruct.library.modifiers.modules.interaction.edible;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierTraitModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Shared stat-driven behavior for all edible tools and armor. */
public enum EdibleModule implements ModifierModule, GeneralInteractionModifierHook, UsingToolModifierHook, OnAttackedModifierHook, TooltipModifierHook {
  INSTANCE;

  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<EdibleModule>defaultHooks(
    ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING, ModifierHooks.ON_ATTACKED, ModifierHooks.TOOLTIP);
  public static final RecordLoadable<EdibleModule> LOADER = new SingletonLoader<>(INSTANCE);
  private static final IJsonPredicate<Item> VALID_TOOLS = ItemPredicate.or(
    ItemPredicate.tag(TinkerTags.Items.INTERACTABLE_CHARGE), ItemPredicate.tag(TinkerTags.Items.ARMOR));

  public static final FloatToolStat HUNGER = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "hunger"), 0xFFF0A8A4, 0, 0, 200, VALID_TOOLS);
  public static final FloatToolStat SATURATION = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "saturation"), 0xFFF0A8A4, 0, 0, 200, VALID_TOOLS);
  public static final FloatToolStat EAT_DURATION = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "eat_duration"), 0xFFF0A8A4, 16, 0, 100, VALID_TOOLS);
  public static final FloatToolStat COUNTER_CHANCE = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "edible_counter_chance"), 0xFFF0A8A4, 0, 0, 1, VALID_TOOLS);
  public static final ModifierModule EDIBLE_TRAIT = new ModifierTraitModule(TinkerModifiers.edible.getId(), 1, false);

  @Override public RecordLoadable<EdibleModule> getLoader() { return LOADER; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    StatsNBT stats = tool.getStats();
    tooltip.add(HUNGER.formatValue(stats.getIntOr(HUNGER, 0)));
    tooltip.add(SATURATION.formatValue(stats.get(SATURATION)));
  }

  @Override
  public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
    if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken() && player.canEat(false) && tool.getStats().getIntOr(HUNGER, 0) > 0) {
      GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  @Override public ItemUseAnimation getUseAction(IToolStackView tool, ModifierEntry modifier) { return ItemUseAnimation.EAT; }
  @Override public int getUseDuration(IToolStackView tool, ModifierEntry modifier) { return tool.getStats().getIntOr(EAT_DURATION, 16); }

  private static void eat(IToolStackView tool, Player player, EquipmentSlot eatenSlot) {
    int hunger = Math.round(ConditionalStatModifierHook.getModifiedStat(tool, player, HUNGER));
    if (hunger <= 0) return;
    float saturation = ConditionalStatModifierHook.getModifiedStat(tool, player, SATURATION);
    player.getFoodData().eat(hunger, saturation);
    Level level = player.level();
    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F,
      1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F);
    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.5F,
      level.getRandom().nextFloat() * 0.1F + 0.9F);

    List<ItemStack> representatives = new ArrayList<>();
    for (ModifierEntry entry : tool.getModifiers()) {
      entry.getHook(ModifierHooks.EDIBLE_EFFECT).onToolEaten(tool, entry, player, eatenSlot, hunger, saturation, representatives);
    }
    if (!representatives.isEmpty()) {
      ModifierUtil.foodConsumer.onConsume(player, representatives, hunger, saturation);
    }
  }

  @Override
  public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (modifier != activeModifier && !entity.level().isClientSide() && tool.getStats().getIntOr(HUNGER, 0) > 0) {
      int duration = tool.getStats().getIntOr(EAT_DURATION, 16);
      if (useDuration - timeLeft == duration && entity instanceof Player player && player.canEat(false)) {
        eat(tool, player, Util.getSlotType(entity.getUsedItemHand()));
      }
    }
  }

  @Override
  public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (!entity.level().isClientSide() && !tool.isBroken() && entity instanceof Player player && player.canEat(false)) {
      int duration = tool.getStats().getIntOr(EAT_DURATION, 16);
      if (tool.getStats().getIntOr(HUNGER, 0) > 0 && useDuration - timeLeft == duration) {
        eat(tool, player, Util.getSlotType(entity.getUsedItemHand()));
      }
    }
  }

  @Override
  public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    if (!tool.isBroken() && tool.hasTag(TinkerTags.Items.ARMOR) && tool.getStats().getIntOr(HUNGER, 0) > 0) {
      LivingEntity entity = context.getEntity();
      if (context.getLevel().getRandom().nextFloat() < ConditionalStatModifierHook.getModifiedStat(tool, entity, COUNTER_CHANCE)
          && entity instanceof Player player && player.canEat(true)) {
        eat(tool, player, slotType);
      }
    }
  }
}

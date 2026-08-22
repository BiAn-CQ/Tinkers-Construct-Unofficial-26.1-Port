package slimeknights.tconstruct.library.modifiers.modules.interaction.edible;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EdibleEffectHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;

import java.util.List;

/** Supplies eating particles and representative food items for integrations. */
public record EdibleRepresentativeItemModule(ItemStackTemplate representativeItem, ModifierCondition<IToolStackView> condition)
  implements ModifierModule, UsingToolModifierHook, EdibleEffectHook, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<EdibleRepresentativeItemModule>defaultHooks(ModifierHooks.TOOL_USING, ModifierHooks.EDIBLE_EFFECT);
  public static final RecordLoadable<EdibleRepresentativeItemModule> LOADER = RecordLoadable.create(
    ItemStackTemplateLoadable.ITEM_NBT.requiredField("representative_item", EdibleRepresentativeItemModule::representativeItem),
    ModifierCondition.TOOL_FIELD, EdibleRepresentativeItemModule::new);

  public EdibleRepresentativeItemModule(ItemLike item) { this(new ItemStackTemplate(item.asItem()), ModifierCondition.ANY_TOOL); }
  @Override public RecordLoadable<EdibleRepresentativeItemModule> getLoader() { return LOADER; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }

  @Override
  public void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot, int hunger, float saturation, List<ItemStack> representativeItems) {
    if (!tool.isBroken() && condition.matches(tool, modifier)) representativeItems.add(representativeItem.create());
  }

  private static void eatEffects(LivingEntity entity, ItemStack stack, int amount) {
    entity.spawnItemParticles(stack, amount);
    RandomSource random = entity.getRandom();
    entity.playSound(SoundEvents.GENERIC_EAT.value(), 0.5f + 0.5f * random.nextInt(2), (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f);
  }

  @Override
  public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (!tool.isBroken() && condition.matches(tool, modifier)) {
      StatsNBT stats = tool.getStats();
      if (stats.getIntOr(EdibleModule.HUNGER, 0) > 0) {
        int elapsed = useDuration - timeLeft;
        int duration = stats.getIntOr(EdibleModule.EAT_DURATION, 16);
        if (elapsed == duration && entity instanceof Player player && player.canEat(false)) eatEffects(entity, representativeItem.create(), 16);
        else if (elapsed < duration && elapsed % 4 == 0 && entity instanceof Player player && player.canEat(false)) eatEffects(entity, representativeItem.create(), 5);
      }
    }
  }

  @Override
  public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (!tool.isBroken() && condition.matches(tool, modifier) && entity instanceof Player player && player.canEat(false)) {
      StatsNBT stats = tool.getStats();
      if (stats.getIntOr(EdibleModule.HUNGER, 0) > 0 && useDuration - timeLeft == stats.getIntOr(EdibleModule.EAT_DURATION, 16)) {
        eatEffects(entity, representativeItem.create(), 5);
      }
    }
  }
}

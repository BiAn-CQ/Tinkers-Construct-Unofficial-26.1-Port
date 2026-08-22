package slimeknights.tconstruct.library.modifiers.modules.interaction.edible;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EdibleEffectHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.EffectCureUtil;

import java.util.List;

/** Removes all effects curable by a configured representative item. */
public record EdibleCureEffectsModule(ItemStackTemplate curativeItem, IJsonPredicate<LivingEntity> holder, ModifierCondition<IToolStackView> condition)
  implements ModifierModule, EdibleEffectHook, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<EdibleCureEffectsModule>defaultHooks(ModifierHooks.EDIBLE_EFFECT);
  public static final RecordLoadable<EdibleCureEffectsModule> LOADER = RecordLoadable.create(
    ItemStackTemplateLoadable.ITEM_NBT.requiredField("curative_item", EdibleCureEffectsModule::curativeItem),
    LivingEntityPredicate.LOADER.defaultField("holder", EdibleCureEffectsModule::holder), ModifierCondition.TOOL_FIELD, EdibleCureEffectsModule::new);
  public EdibleCureEffectsModule(ItemLike item) { this(new ItemStackTemplate(item.asItem()), LivingEntityPredicate.ANY, ModifierCondition.ANY_TOOL); }
  @Override public RecordLoadable<EdibleCureEffectsModule> getLoader() { return LOADER; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }
  @Override public void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot, int hunger, float saturation, List<ItemStack> items) {
    if (condition.matches(tool, modifier) && holder.matches(player)) EffectCureUtil.removeEffectsCuredBy(player, curativeItem.create());
  }
}

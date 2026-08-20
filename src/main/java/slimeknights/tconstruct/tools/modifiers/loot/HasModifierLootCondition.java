package slimeknights.tconstruct.tools.modifiers.loot;

import com.mojang.serialization.MapCodec;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

/** Condition to check if a held tool has the given modifier */
@RequiredArgsConstructor
public class HasModifierLootCondition implements LootItemCondition {
  public static final MapCodec<HasModifierLootCondition> CODEC = Identifier.CODEC.fieldOf("modifier")
    .xmap(id -> new HasModifierLootCondition(new ModifierId(id)), condition -> condition.modifier.location());
  private final ModifierId modifier;


  @Override
  public boolean test(LootContext context) {
    ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
    return tool instanceof ItemStack stack && stack.is(TinkerTags.Items.MODIFIABLE) && ModifierUtil.getModifierLevel(stack, modifier) > 0;
  }

  @Override
  public MapCodec<? extends LootItemCondition> codec() {
    return CODEC;
  }

}

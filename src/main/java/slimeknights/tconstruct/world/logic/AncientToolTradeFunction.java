package slimeknights.tconstruct.world.logic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/** Replaces a trade's placeholder result with a randomized ancient tool. */
public class AncientToolTradeFunction extends LootItemConditionalFunction {
  public static final MapCodec<AncientToolTradeFunction> CODEC = RecordCodecBuilder.mapCodec(
    instance -> commonFields(instance).apply(instance, AncientToolTradeFunction::new));

  private AncientToolTradeFunction(List<LootItemCondition> conditions) {
    super(conditions);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    return AncientToolItemListing.createResult(context.getRandom());
  }

  @Override
  public MapCodec<? extends LootItemConditionalFunction> codec() {
    return CODEC;
  }
}

package slimeknights.tconstruct.world.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.common.config.Config;

/** Enables one weighted ancient-tool trade slot when allowed by the common config. */
public record AncientToolTradeWeightCondition(int minimum) implements LootItemCondition {
  public static final int MAX_WEIGHT = 100;
  public static final MapCodec<AncientToolTradeWeightCondition> CODEC = Codec.intRange(1, MAX_WEIGHT)
    .fieldOf("minimum").xmap(AncientToolTradeWeightCondition::new, AncientToolTradeWeightCondition::minimum);

  @Override
  public boolean test(LootContext context) {
    return Config.COMMON.wandererAncientToolWeight.get() >= minimum;
  }

  @Override
  public MapCodec<? extends LootItemCondition> codec() {
    return CODEC;
  }
}

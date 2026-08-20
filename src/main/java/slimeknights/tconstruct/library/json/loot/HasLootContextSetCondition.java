package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.shared.TinkerCommons;


/**
 * Loot condition that only runs if all required values in the given loot context set are present. Good heuristic for using that set.
 * TODO: migrate to Mantle
 */
public record HasLootContextSetCondition(ContextKeySet set) implements LootItemCondition {
  public static final MapCodec<HasLootContextSetCondition> CODEC = LootContextParamSets.CODEC.fieldOf("set")
    .xmap(HasLootContextSetCondition::new, HasLootContextSetCondition::set);
  /** Creates a new builder instance */
  public static Builder builder(ContextKeySet set) {
    return new Builder(set);
  }

  @Override
  public MapCodec<? extends LootItemCondition> codec() {
    return CODEC;
  }


  @Override
  public boolean test(LootContext context) {
    for (ContextKey<?> param : set.required()) {
      if (!context.hasParameter(param)) {
        return false;
      }
    }
    return true;
  }

  /** Builder logic for this condition */
  public record Builder(ContextKeySet set) implements LootItemCondition.Builder {
    @Override
    public LootItemCondition build() {
      return new HasLootContextSetCondition(set);
    }
  }

}

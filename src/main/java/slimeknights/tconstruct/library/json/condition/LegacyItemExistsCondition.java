package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.TConstruct;

/** Compatibility condition for legacy Forge data using {@code forge:item_exists}. */
public record LegacyItemExistsCondition(Identifier item) implements ICondition {
  public static final Identifier ID = TConstruct.getResource("item_exists");
  public static final MapCodec<LegacyItemExistsCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
    .group(Identifier.CODEC.fieldOf("item").forGetter(LegacyItemExistsCondition::item))
    .apply(instance, LegacyItemExistsCondition::new));

  @Override
  public boolean test(IContext context) {
    ResourceKey<net.minecraft.world.item.Item> key = ResourceKey.create(Registries.ITEM, item);
    return context.registryAccess().holder(key).map(Holder::isBound).orElse(false);
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }
}

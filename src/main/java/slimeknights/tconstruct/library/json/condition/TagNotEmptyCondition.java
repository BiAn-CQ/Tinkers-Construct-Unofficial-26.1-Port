package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagFilledCondition} */
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class TagNotEmptyCondition<T> implements LootItemCondition {
  private static final Identifier NAME = TConstruct.getResource("tag_not_empty");
  public static final MapCodec<TagNotEmptyCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Identifier.CODEC.fieldOf("registry").forGetter(value -> value.tag.registry().identifier()),
    Identifier.CODEC.fieldOf("tag").forGetter(value -> value.tag.location())
  ).apply(instance, TagNotEmptyCondition::read));
  private final TagKey<T> tag;

  @SuppressWarnings("removal")
  public Identifier getID() {
    return NAME;
  }

  @Override
  public MapCodec<? extends LootItemCondition> codec() {
    return CODEC;
  }

  private static TagNotEmptyCondition<?> read(Identifier registry, Identifier tag) {
    return new TagNotEmptyCondition<>(TagKey.create(ResourceKey.createRegistryKey(registry), tag));
  }

  @Override
  public boolean test(LootContext context) {
    Registry<T> registry = RegistryHelper.getRegistry(tag.registry());
    return registry != null && registry.getTagOrEmpty(tag).iterator().hasNext();
  }

}

package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Item ingredient matching items with a block form in the given tag. */
public class BlockTagIngredient implements ICustomIngredient {
  public static final LoadableIngredientSerializer<BlockTagIngredient> SERIALIZER = new LoadableIngredientSerializer<>(RecordLoadable.create(
    Loadables.BLOCK_TAG.requiredField("tag", ingredient -> ingredient.tag),
    BlockTagIngredient::new));
  public static final IngredientType<BlockTagIngredient> TYPE = new IngredientType<>(SERIALIZER.codec());

  private final TagKey<Block> tag;
  @Nullable
  private Set<Item> matchingItems;

  public BlockTagIngredient(TagKey<Block> tag) {
    this.tag = tag;
  }

  /** Creates a native ingredient matching item forms of blocks in the given tag. */
  public static Ingredient of(TagKey<Block> tag) {
    return new BlockTagIngredient(tag).toVanilla();
  }

  private Set<Item> getMatchingItems() {
    if (matchingItems == null) {
      matchingItems = StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)
        .map(holder -> holder.value().asItem())
        .filter(item -> item != Items.AIR)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return matchingItems;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && getMatchingItems().contains(stack.getItem());
  }

  @Override
  public Stream<Holder<Item>> items() {
    return getMatchingItems().stream().map(BuiltInRegistries.ITEM::wrapAsHolder);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  public JsonElement toJson() {
    JsonObject json = SERIALIZER.serialize(this);
    json.addProperty("type", TConstruct.getResource("block_tag").toString());
    return json;
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof BlockTagIngredient other && tag.equals(other.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag);
  }
}

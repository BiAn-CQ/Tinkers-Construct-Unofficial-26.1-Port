package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient that only matches tools exposing a specific definition hook. */
public final class ToolHookIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("tool_hook");
  public static final IngredientType<ToolHookIngredient> TYPE = new IngredientType<>(
    LoadableIngredientSerializer.mapCodec(ToolHookIngredient::parse, ToolHookIngredient::serialize));

  private final TagKey<Item> tag;
  private final ModuleHook<?> hook;

  private ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
    this.tag = tag;
    this.hook = hook;
  }

  private static ToolHookIngredient parse(JsonObject json) {
    return new ToolHookIngredient(
      Loadables.ITEM_TAG.getOrDefault(json, "tag", TinkerTags.Items.MODIFIABLE),
      ToolHooks.LOADER.getIfPresent(json, "hook"));
  }

  private static JsonObject serialize(ToolHookIngredient ingredient) {
    JsonObject json = new JsonObject();
    json.addProperty("tag", ingredient.tag.location().toString());
    json.addProperty("hook", ingredient.hook.getId().toString());
    return json;
  }

  public static Ingredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook).toVanilla();
  }

  public static Ingredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && stack.is(tag) && stack.getItem() instanceof IModifiable modifiable
      && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public Stream<Holder<Item>> items() {
    List<Holder<Item>> stacks = new ArrayList<>();
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (holder.value() instanceof IModifiable modifiable
          && modifiable.getToolDefinition().getData().getHooks().hasHook(hook)) {
        stacks.add(holder);
      }
    }
    if (stacks.isEmpty()) {
      stacks.add(BuiltInRegistries.ITEM.wrapAsHolder(Blocks.BARRIER.asItem()));
    }
    return stacks.stream();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof ToolHookIngredient other
      && tag.equals(other.tag) && hook.equals(other.hook);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag, hook);
  }
}

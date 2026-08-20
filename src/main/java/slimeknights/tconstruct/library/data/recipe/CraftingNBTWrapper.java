package slimeknights.tconstruct.library.data.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;

import javax.annotation.Nullable;

/** Native 1.21 recipe output decorator for legacy crafting-result NBT. */
public final class CraftingNBTWrapper {
  private CraftingNBTWrapper() {}

  public static RecipeOutput wrap(RecipeOutput output, CompoundTag nbt, HolderLookup.Provider registries) {
    CompoundTag data = nbt.copy();
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void includeRootAdvancement() {
        output.includeRootAdvancement();
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
        accept(id, recipe, advancement, new ICondition[0]);
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (!(recipe instanceof CraftingRecipe crafting)) {
          throw new IllegalArgumentException("Crafting result NBT requires a crafting recipe, got " + recipe.getClass().getName());
        }
        ItemStack result = crafting.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
        applyLegacyData(result, data, registries);
        Recipe<?> wrapped;
        if (recipe instanceof ShapedRetexturedRecipe retextured) {
          wrapped = retextured.withResult(result);
        } else if (recipe instanceof ShapedMaterialsRecipe materials) {
          wrapped = materials.withResult(result);
        } else if (recipe instanceof ShapedRecipe shaped) {
          wrapped = new ShapedRecipe(new Recipe.CommonInfo(shaped.showNotification()),
            new CraftingRecipe.CraftingBookInfo(shaped.category(), shaped.group()), shaped.pattern,
            ItemStackTemplate.fromNonEmptyStack(result));
        } else if (recipe instanceof ShapelessRecipe shapeless) {
          wrapped = new ShapelessRecipe(new Recipe.CommonInfo(shapeless.showNotification()),
            new CraftingRecipe.CraftingBookInfo(shapeless.category(), shapeless.group()),
            ItemStackTemplate.fromNonEmptyStack(result), shapeless.placementInfo().ingredients());
        } else {
          throw new IllegalArgumentException("Crafting result NBT requires a shaped or shapeless recipe, got " + recipe.getClass().getName());
        }
        output.accept(id, wrapped, advancement, conditions);
      }
    };
  }

  private static void applyLegacyData(ItemStack stack, CompoundTag source, HolderLookup.Provider registries) {
    CompoundTag remaining = source.copy();
    if (remaining.contains("display")) {
      CompoundTag display = remaining.getCompoundOrEmpty("display");
      if (display.contains("Name")) {
        Component name = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,
          JsonParser.parseString(display.getStringOr("Name", ""))).result().orElse(null);
        if (name != null) {
          stack.set(DataComponents.CUSTOM_NAME, name);
        }
        display.remove("Name");
      }
      if (display.isEmpty()) {
        remaining.remove("display");
      }
    }
    if (!remaining.isEmpty()) {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(remaining));
    }
  }
}

package slimeknights.tconstruct.library.recipe.casting;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;

import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;

import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid NBT to the item
 */
public class PotionCastingRecipe implements ICastingRecipe, IMultiRecipe<DisplayCastingRecipe> {
  protected static final LoadableField<FluidIngredient, PotionCastingRecipe> FLUID_FIELD = FluidIngredient.LOADABLE.requiredField("fluid", r -> r.fluid);
  protected static final LoadableField<Integer, PotionCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime);
  public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("bottle", r -> r.bottle),
    FLUID_FIELD,
    Loadables.ITEM.requiredField("result", r -> r.result),
    COOLING_TIME_FIELD,
    PotionCastingRecipe::new);

  @Getter
  protected final TypeAwareRecipeSerializer serializer;
  @Getter
  protected final Identifier id;
  @Getter
  protected final String group;
  /** Input on the casting table, always consumed */
  protected final Ingredient bottle;
  /** Potion ingredient, typically just the potion tag */
  protected final FluidIngredient fluid;
  /** Potion item result, will be given the proper NBT */
  protected final Item result;
  /** Cooling time for this recipe, used for tipped arrows */
  protected final int coolingTime;

  public Identifier getId() {
    return id;
  }

  @Override
  public RecipeSerializer getSerializer() {
    return serializer.serializer();
  }

  public PotionCastingRecipe(TypeAwareRecipeSerializer serializer, Identifier id, String group, Ingredient bottle, FluidIngredient fluid, Item result, int coolingTime) {
    this.serializer = serializer;
    this.id = id;
    this.group = group;
    this.bottle = bottle;
    this.fluid = fluid;
    this.result = result;
    this.coolingTime = coolingTime;
    CastingRecipeLookup.registerCastable(result);
  }

  @Override
  public RecipeType getType() {
    return serializer.getType();
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    return bottle.test(inv.getStack()) && fluid.test(inv.getFluid());
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return fluid.getAmount(inv.getFluid());
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return coolingTime;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = new ItemStack(this.result);
    PotionContents potion = PotionFluidType.getPotionContents(inv.getFluidStack());
    if (!potion.equals(PotionContents.EMPTY)) {
      result.set(DataComponents.POTION_CONTENTS, potion);
    }
    CustomData customData = inv.getFluidStack().get(DataComponents.CUSTOM_DATA);
    if (customData != null) {
      result.set(DataComponents.CUSTOM_DATA, customData);
    }
    return result;
  }


  /* JEI */
  protected List<DisplayCastingRecipe> displayRecipes = null;

  @Override
  public List<DisplayCastingRecipe> getRecipes(HolderLookup.Provider access) {
    if (displayRecipes == null) {
      // create a subrecipe for every potion variant
      List<ItemStack> bottles = List.of(slimeknights.tconstruct.library.recipe.TinkerIngredients.getItems(bottle));
      displayRecipes = BuiltInRegistries.POTION.stream().map(BuiltInRegistries.POTION::wrapAsHolder)
        .map(potion -> {
          ItemStack result = new ItemStack(this.result);
          result.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
          return new DisplayCastingRecipe(getId(), getType(), bottles, fluid.getFluids().stream()
                                                              .map(fluid -> slimeknights.tconstruct.library.utils.FluidStackDataUtil.createPotion(fluid.getFluid(), fluid.getAmount(), potion))
                                                              .toList(),
                                          result, coolingTime, true);
        }).toList();
    }
    return displayRecipes;
  }


  /* Recipe interface methods */

  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(TinkerIngredients.EMPTY, bottle);
  }

  /** @deprecated use {@link #assemble(Container, HolderLookup.Provider)} */
  @Deprecated
  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.result);
  }
}

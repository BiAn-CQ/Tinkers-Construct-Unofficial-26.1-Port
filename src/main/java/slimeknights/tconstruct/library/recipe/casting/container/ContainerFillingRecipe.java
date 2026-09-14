package slimeknights.tconstruct.library.recipe.casting.container;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;

import java.util.Collections;
import java.util.List;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<ContainerFillingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IntLoadable.FROM_ONE.requiredField("fluid_amount", r -> r.fluidAmount),
    Loadables.ITEM.requiredField("container", r -> r.container),
    ContainerFillingRecipe::new);

  @Getter
  private final TypeAwareRecipeSerializer serializer;
  @Getter
  private final Identifier id;
  @Getter
  private final String group;
  private final int fluidAmount;
  private final Item container;

  @Override
  public RecipeSerializer getSerializer() {
    return serializer.serializer();
  }

  public Identifier getId() {
    return id;
  }

  @Override
  public RecipeType getType() {
    return serializer.getType();
  }

  private static ResourceHandler<FluidResource> getFluidHandler(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    ItemAccess access = ItemAccess.forStack(stack).oneByOne();
    return access.getCapability(Capabilities.Fluid.ITEM);
  }

  /** Inserts fluid transactionally, committing only for the assembly path. */
  private static int insertFluid(ResourceHandler<FluidResource> handler, FluidStack fluid, boolean execute) {
    if (fluid.isEmpty()) {
      return 0;
    }
    try (Transaction transaction = Transaction.open(null)) {
      int inserted = handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
      if (execute && inserted > 0) {
        transaction.commit();
      }
      return inserted;
    }
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    Fluid fluid = inv.getFluid();
    ResourceHandler<FluidResource> handler = getFluidHandler(inv.getStack());
    return handler == null ? 0 : insertFluid(handler, new FluidStack(fluid, this.fluidAmount), false);
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
    return 5;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    ItemStack stack = inv.getStack();
    Fluid fluid = inv.getFluid();
    ResourceHandler<FluidResource> handler = getFluidHandler(stack);
    return stack.getItem() == this.container.asItem() && handler != null
           && insertFluid(handler, new FluidStack(fluid, this.fluidAmount), false) > 0;
  }

  /** @deprecated use {@link ICastingRecipe#assemble(Container, HolderLookup.Provider)} */
  @Override
  @Deprecated
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.container);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack stack = inv.getStack().copy();
    ResourceHandler<FluidResource> handler = getFluidHandler(stack);
    if (handler == null) {
      return stack;
    }
    insertFluid(handler, inv.getFluidStack().copyWithAmount(this.fluidAmount), true);
    return stack;
  }

  /* Display */
  /** Cache of items to display for this container */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(HolderLookup.Provider access) {
    if (displayRecipes == null) {
      List<FluidStack> fluids = BuiltInRegistries.FLUID.stream()
        .filter(fluid -> {
          if (fluid.isSource(fluid.defaultFluidState())
              && !fluid.builtInRegistryHolder().is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
            try {
              var bucket = fluid.getBucket();
              return bucket != Items.AIR && !bucket.builtInRegistryHolder().is(TinkerTags.Items.HIDDEN_IN_RECIPE_VIEWERS);
            } catch (Exception ignored) {
              // Some fluid implementations throw when they do not provide a bucket.
            }
          }
          return false;
        }).map(fluid -> new FluidStack(fluid, fluidAmount)).toList();
      List<ItemStack> results = fluids.stream().map(fluid -> {
        ItemStack stack = new ItemStack(container);
        ResourceHandler<FluidResource> handler = getFluidHandler(stack);
        if (handler != null) {
          insertFluid(handler, fluid, true);
        }
        return stack;
      }).toList();
      displayRecipes = List.of(DisplayCastingRecipe.type(getType()).id(getId())
        .cast(new ItemStack(container)).consumed()
        .fluids(fluids).results(results).linkFluidsToOutput().coolingTime(5).build());
    }
    return displayRecipes;
  }
}

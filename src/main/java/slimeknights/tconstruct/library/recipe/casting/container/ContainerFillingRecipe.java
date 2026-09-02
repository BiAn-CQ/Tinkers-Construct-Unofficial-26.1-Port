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
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;

import java.util.Collections;
import java.util.List;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<DisplayCastingRecipe> {
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
  private List<DisplayCastingRecipe> displayRecipes = null;

  @Override
  public List<DisplayCastingRecipe> getRecipes(HolderLookup.Provider access) {
    if (displayRecipes == null) {
      List<ItemStack> casts = Collections.singletonList(new ItemStack(container));
      displayRecipes = BuiltInRegistries.FLUID.stream()
                                             .filter(fluid -> fluid.getBucket() != Items.AIR && fluid.isSource(fluid.defaultFluidState()))
                                               .map(fluid -> {
                                                 FluidStack fluidStack = new FluidStack(fluid, fluidAmount);
                                                 ItemStack stack = new ItemStack(container);
                                               ResourceHandler<FluidResource> handler = getFluidHandler(stack);
                                               if (handler != null) {
                                                 insertFluid(handler, fluidStack, true);
                                               }
                                               return new DisplayCastingRecipe(getId(), getType(), casts, Collections.singletonList(fluidStack), stack, 5, true);
                                             })
                                             .toList();
    }
    return displayRecipes;
  }
}

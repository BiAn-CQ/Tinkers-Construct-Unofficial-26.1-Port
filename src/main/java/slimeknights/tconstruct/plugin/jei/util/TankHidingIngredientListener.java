package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Keeps filled container variants synchronized with runtime JEI fluid removals. */
public record TankHidingIngredientListener(IIngredientManager manager, List<Item> tanks)
    implements IIngredientManager.IIngredientListener {
  private List<ItemStack> getTanks(Collection<? extends ITypedIngredient<?>> ingredients) {
    List<ItemStack> result = new ArrayList<>();
    for (ITypedIngredient<?> ingredient : ingredients) {
      FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).orElse(FluidStack.EMPTY);
      if (fluid.isEmpty()) continue;
      for (Item item : tanks) {
        ItemStack stack = new ItemStack(item);
        ItemAccess access = ItemAccess.forStack(stack);
        var handler = stack.getCapability(Capabilities.Fluid.ITEM, access);
        if (handler != null && IMeltingRecipe.insert(handler, fluid) > 0) {
          result.add(access.getResource().toStack());
        }
      }
    }
    return result;
  }

  @Override
  public <V> void onIngredientsAdded(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> stacks = getTanks(ingredients);
    if (!stacks.isEmpty()) manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
  }

  @Override
  public <V> void onIngredientsRemoved(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> stacks = getTanks(ingredients);
    if (!stacks.isEmpty()) manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
  }
}

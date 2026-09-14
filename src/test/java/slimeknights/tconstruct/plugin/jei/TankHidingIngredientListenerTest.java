package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.plugin.jei.util.TankHidingIngredientListener;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

class TankHidingIngredientListenerTest extends slimeknights.tconstruct.test.CoreTestBootstrap {
  @Test
  @SuppressWarnings("unchecked")
  void itemCallbacksDoNotRecursivelyModifyIngredients() {
    IIngredientManager manager = mock(IIngredientManager.class);
    ITypedIngredient<ItemStack> ingredient = mock(ITypedIngredient.class);
    when(ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)).thenReturn(Optional.empty());
    var listener = new TankHidingIngredientListener(manager, List.of(Items.BUCKET));
    listener.onIngredientsAdded(null, List.of(ingredient));
    listener.onIngredientsRemoved(null, List.of(ingredient));
    verifyNoInteractions(manager);
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyFluidDoesNotCreateContainerVariants() {
    IIngredientManager manager = mock(IIngredientManager.class);
    ITypedIngredient<FluidStack> ingredient = mock(ITypedIngredient.class);
    when(ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)).thenReturn(Optional.of(FluidStack.EMPTY));
    var listener = new TankHidingIngredientListener(manager, List.of(Items.BUCKET));
    listener.onIngredientsAdded(null, List.of(ingredient));
    listener.onIngredientsRemoved(null, List.of(ingredient));
    verifyNoInteractions(manager);
  }
}

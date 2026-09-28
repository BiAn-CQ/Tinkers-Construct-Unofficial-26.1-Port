package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.utils.TinkerRecipeHelper;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClientMeltingQueryTest extends BaseMcTest {
  @Test
  void clientInventoryUpdateComputesHeatFromSynchronizedRecipe() {
    Level client = mock(Level.class);
    when(client.isClientSide()).thenReturn(true);
    when(client.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    MantleBlockEntity parent = mock(MantleBlockEntity.class);
    when(parent.getLevel()).thenReturn(client);
    IMeltingRecipe recipe = mock(IMeltingRecipe.class);
    when(recipe.getType()).thenReturn(TinkerRecipeTypes.MELTING.get());
    when(recipe.matches(any(), eq(client))).thenReturn(true);
    when(recipe.getTime(any())).thenReturn(12);
    when(recipe.getTemperature(any(slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.class))).thenReturn(800);
    var holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("test:melting")), recipe);
    TinkerRecipeHelper.setClientRecipeMap(RecipeMap.create(List.of(holder)));
    try {
      MeltingModule module = new MeltingModule(parent, ignored -> false, null, -1, ignored -> {});
      module.updateStackFromInventory(new ItemStack(Items.IRON_INGOT));
      assertThat(module.getRequiredTime()).isEqualTo(120);
      assertThat(module.getRequiredTemp()).isEqualTo(800);
    } finally {
      TinkerRecipeHelper.clearClientRecipeMap();
    }
  }
}

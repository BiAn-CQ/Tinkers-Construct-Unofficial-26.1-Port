package slimeknights.tconstruct.tables.block.entity.inventory;

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
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.utils.TinkerRecipeHelper;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClientMaterialQueryTest extends BaseMcTest {
  @Test
  void bothWrappersFindSynchronizedMaterialWithoutServerManager() {
    Level client = mock(Level.class);
    when(client.isClientSide()).thenReturn(true);
    when(client.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    MaterialRecipe recipe = mock(MaterialRecipe.class);
    when(recipe.getType()).thenReturn(TinkerRecipeTypes.MATERIAL.get());
    when(recipe.matches(any(), eq(client))).thenReturn(true);
    var holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("test:material")), recipe);
    TinkerRecipeHelper.setClientRecipeMap(RecipeMap.create(List.of(holder)));
    try {
      ItemStack input = new ItemStack(Items.IRON_INGOT);
      PartBuilderBlockEntity builder = mock(PartBuilderBlockEntity.class);
      when(builder.getLevel()).thenReturn(client);
      when(builder.getItem(PartBuilderBlockEntity.MATERIAL_SLOT)).thenReturn(input);
      assertThat(new PartBuilderContainerWrapper(builder).getMaterial()).isSameAs(recipe);

      TinkerStationBlockEntity station = mock(TinkerStationBlockEntity.class);
      when(station.getLevel()).thenReturn(client);
      when(station.getInputCount()).thenReturn(1);
      when(station.getItem(TinkerStationBlockEntity.INPUT_SLOT)).thenReturn(input);
      assertThat(new TinkerStationContainerWrapper(station).getInputMaterial(0)).isSameAs(recipe);
    } finally {
      TinkerRecipeHelper.clearClientRecipeMap();
    }
  }
}

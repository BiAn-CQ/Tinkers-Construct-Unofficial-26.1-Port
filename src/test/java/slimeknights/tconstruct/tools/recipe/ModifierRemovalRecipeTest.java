package slimeknights.tconstruct.tools.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierSalvage;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerRecipeHelper;
import slimeknights.tconstruct.test.CoreTestBootstrap;


import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ModifierRemovalRecipeTest extends CoreTestBootstrap {
  @AfterEach
  void cleanup() {
    TinkerRecipeHelper.clearClientRecipeMap();
    RecipeCacheInvalidator.reload(false);
  }

  @Test
  void sharpnessRemovalRestoresSlotFromSyncedRecipeAfterLegacyCacheIsCleared() {
    var modifier = new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct", "sharpness");
    var id = Identifier.fromNamespaceAndPath("tconstruct", "test_sharpness_salvage");
    var salvage = new ModifierSalvage(id, Ingredient.of(Items.IRON_SWORD), 1, modifier,
      ModifierEntry.VALID_LEVEL, new SlotType.SlotCount(SlotType.UPGRADE, 1));
    var holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), salvage);
    TinkerRecipeHelper.setClientRecipeMap(RecipeMap.create(List.of(holder)));
    RecipeCacheInvalidator.reload(false);

    ToolStack tool = ToolStack.from(Items.IRON_SWORD, ToolDefinition.EMPTY, new CompoundTag());
    tool.getPersistentData().setSlots(SlotType.UPGRADE, 2);
    tool.addModifier(modifier, 1);
    ItemStack stack = tool.createStack();
    ITinkerableContainer inv = mock(ITinkerableContainer.class);
    when(inv.getTinkerable()).thenReturn(tool);
    when(inv.getTinkerableStack()).thenReturn(stack);
    var recipe = new ModifierRemovalRecipe(id, "modifiers", SizedIngredient.of(Ingredient.of(Items.IRON_SWORD)),
      List.of(), List.of(), ModifierPredicate.ANY);
    Level level = mock(Level.class);
    when(level.isClientSide()).thenReturn(true);
    when(level.registryAccess()).thenReturn(net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
    var entry = mock(ModifierEntry.class);
    when(entry.getId()).thenReturn(modifier);
    when(entry.getLevel()).thenReturn(1);
    when(entry.getModifier()).thenReturn(new slimeknights.tconstruct.library.modifiers.Modifier());

    var legacy = recipe.getResult(inv, entry);
    assertThat(legacy.isSuccess()).isTrue();
    assertThat(legacy.getResult().getTool().getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(2);
    var result = recipe.getResult(inv, entry, level);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getResult().getTool().getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(3);
    assertThat(ToolStack.from(result.getResult().getStack()).getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(3);
    assertThat(result.getResult().getTool().getUpgrades().getLevel(modifier)).isZero();
    assertThat(tool.getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(2);

    var server = mock(net.minecraft.server.MinecraftServer.class);
    var serverLevel = mock(net.minecraft.server.level.ServerLevel.class);
    var manager = mock(RecipeManager.class);
    when(serverLevel.getServer()).thenReturn(server);
    when(server.getRecipeManager()).thenReturn(manager);
    when(manager.getRecipes()).thenReturn(List.of(holder));
    var serverResult = recipe.getResult(inv, entry, serverLevel);
    assertThat(serverResult.isSuccess()).isTrue();
    assertThat(ToolStack.from(serverResult.getResult().getStack()).getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(3);
    when(manager.getRecipes()).thenReturn(List.of());
    var noServerRecipe = recipe.getResult(inv, entry, serverLevel);
    assertThat(noServerRecipe.getResult().getTool().getPersistentData().getSlots(SlotType.UPGRADE)).isEqualTo(2);

  }
}

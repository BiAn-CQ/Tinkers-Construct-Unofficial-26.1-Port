package slimeknights.tconstruct.tables.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PartBuilderToolRecycleTest extends CoreTestBootstrap {
  private static final MaterialId VALID = new MaterialId("test", "valid");
  private static final MaterialId INVALID = new MaterialId("test", "invalid");

  private IMaterialItem part(Item item) {
    var part = mock(IMaterialItem.class);
    when(part.asItem()).thenReturn(item);
    when(part.canUseMaterial(VALID)).thenReturn(true);
    return part;
  }

  private PartBuilderToolRecycle recipe(IMaterialItem... parts) {
    return new PartBuilderToolRecycle(Identifier.fromNamespaceAndPath("test", "recycle"), null, null, List.of(parts));
  }

  @Test
  void patternButtonsOmitInvalidMaterialsAndEmptyPartSlots() {
    var part = part(Items.FLINT);
    var stack = new MaterialIdNBT(List.of(INVALID, VALID, VALID)).updateStack(new ItemStack(Items.STICK));
    var inventory = mock(IPartBuilderContainer.class);
    when(inventory.getStack()).thenReturn(stack);

    assertThat(recipe(part, part, IMaterialItem.EMPTY).getPatterns(inventory).toList())
      .containsExactly(Pattern.fromItem(part));
  }

  @Test
  void aRepeatedPartCanUseTheLaterValidMaterial() {
    var part = part(Items.FLINT);
    var inventory = mock(IPartBuilderContainer.class);
    var stack = new ItemStack(Items.STICK);
    var tool = mock(ToolStack.class);
    var definition = mock(ToolDefinition.class);
    var materials = MaterialNBT.of(MaterialVariant.of(INVALID, ""), MaterialVariant.of(VALID, ""));
    var result = new ItemStack(Items.FLINT);
    when(inventory.getStack()).thenReturn(stack);
    when(tool.getDefinition()).thenReturn(definition);
    when(tool.getMaterials()).thenReturn(materials);
    when(tool.getMaterial(anyInt())).thenAnswer(call -> materials.get(call.getArgument(0)));
    when(part.withMaterial(VALID)).thenReturn(result);
    try (var tools = mockStatic(ToolStack.class); var stats = mockStatic(ToolMaterialHook.class)) {
      tools.when(() -> ToolStack.from(stack)).thenReturn(tool);
      stats.when(() -> ToolMaterialHook.stats(definition))
        .thenReturn(List.of(StatlessMaterialStats.CUIRASS.getIdentifier(), StatlessMaterialStats.CUIRASS.getIdentifier()));

      assertThat(recipe(part, part).assemble(inventory, null, Pattern.fromItem(part))).isSameAs(result);
    }
  }

  @Test
  void invalidRemainingPartsCannotBeReturnedAsRandomLeftovers() {
    var selected = part(Items.FLINT);
    var invalid = part(Items.IRON_INGOT);
    var inventory = mock(IPartBuilderContainer.class);
    var stack = new ItemStack(Items.STICK);
    var tool = mock(ToolStack.class);
    var definition = mock(ToolDefinition.class);
    when(inventory.getStack()).thenReturn(stack);
    when(tool.getDefinition()).thenReturn(definition);
    when(tool.getMaterial(1)).thenReturn(MaterialVariant.of(INVALID, ""));
    when(invalid.withMaterial(INVALID)).thenReturn(new ItemStack(Items.IRON_INGOT));
    try (var tools = mockStatic(ToolStack.class); var stats = mockStatic(ToolMaterialHook.class)) {
      tools.when(() -> ToolStack.from(stack)).thenReturn(tool);
      stats.when(() -> ToolMaterialHook.stats(definition))
        .thenReturn(List.of(StatlessMaterialStats.CUIRASS.getIdentifier(), StatlessMaterialStats.CUIRASS.getIdentifier()));

      assertThat(recipe(selected, invalid).getLeftover(inventory, Pattern.fromItem(selected))).isSameAs(ItemStack.EMPTY);
      verify(invalid, never()).withMaterial(any());
    }
  }

  @Test
  void optionalMaterialItemsRoundTripTheEmptySlotAsAir() {
    assertThat(TinkerLoadables.OPTIONAL_MATERIAL_ITEM.parseString("minecraft:air", "parts", TypedMap.EMPTY))
      .isSameAs(IMaterialItem.EMPTY);
    assertThat(TinkerLoadables.OPTIONAL_MATERIAL_ITEM.getString(IMaterialItem.EMPTY)).isEqualTo("minecraft:air");
    assertThat(IMaterialItem.EMPTY.canUseMaterial(VALID)).isFalse();
  }
}

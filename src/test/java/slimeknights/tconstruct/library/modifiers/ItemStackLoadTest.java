package slimeknights.tconstruct.library.modifiers;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionDataBuilder;
import slimeknights.tconstruct.library.tools.definition.module.build.SetStatsModule;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerTools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ItemStackLoadTest extends BaseMcTest {
  @BeforeAll
  static void initItems() {
    MaterialItemFixture.init();
  }

  private static ItemStack reload(ItemStack stack) {
    var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(NbtOps.INSTANCE);
    var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
    return ItemStack.CODEC.parse(ops, encoded).getOrThrow();
  }

  @Test
  void savedMaterialPartResolvesRedirectAndPreservesVariant() {
    var oldId = new MaterialId("test", "old_material");
    var newId = new MaterialId("test", "new_material");
    var originalId = MaterialVariantId.create(oldId, "variant");
    ItemStack stack = MaterialItemFixture.MATERIAL_ITEM.withMaterialForDisplay(originalId);
    IMaterialRegistry registry = mock(IMaterialRegistry.class);
    when(registry.resolve(oldId)).thenReturn(newId);
    try (var materials = mockStatic(MaterialRegistry.class)) {
      materials.when(MaterialRegistry::isFullyLoaded).thenReturn(true);
      materials.when(MaterialRegistry::getInstance).thenReturn(registry);
      assertThat(MaterialItemFixture.MATERIAL_ITEM.getMaterial(reload(stack)))
        .isEqualTo(MaterialVariantId.create(newId, "variant"));
      assertThat(MaterialItemFixture.MATERIAL_ITEM.getMaterial(stack.copy())).isEqualTo(originalId);
      var buffer = registryBuffer();
      try {
        ItemStack.STREAM_CODEC.encode(buffer, stack);
        assertThat(MaterialItemFixture.MATERIAL_ITEM.getMaterial(ItemStack.STREAM_CODEC.decode(buffer))).isEqualTo(originalId);
      } finally {
        buffer.release();
      }
    }
  }

  @Test
  void savedToolRebuildsFromCurrentDefinitionWithoutLosingPersistentData() {
    var definition = TinkerTools.pickaxe.get().getToolDefinition();
    var oldDefinition = definition.getData();
    boolean oldLoaded = ModifierManager.INSTANCE.dynamicModifiersLoaded;
    ItemStack stack = new ItemStack(TinkerTools.pickaxe.get());
    ItemStackDataUtil.updateTag(stack, tag -> {
      tag.put(ToolStack.TAG_STATS, StatsNBT.builder().set(ToolStats.DURABILITY, 25).build().serializeToNBT());
      tag.putString("test:preserved", "kept");
    });
    try (var materials = mockStatic(MaterialRegistry.class); var tags = mockStatic(TinkerTags.class)) {
      definition.setData(ToolDefinitionDataBuilder.builder()
        .module(new SetStatsModule(StatsNBT.builder().set(ToolStats.DURABILITY, 150).build())).build());
      ModifierManager.INSTANCE.dynamicModifiersLoaded = true;
      materials.when(MaterialRegistry::isFullyLoaded).thenReturn(true);
      tags.when(TinkerTags::isTagsLoaded).thenReturn(true);
      ItemStack loaded = reload(stack);
      assertThat(ToolStack.from(loaded).getStats().get(ToolStats.DURABILITY)).isEqualTo(150f);
      assertThat(ItemStackDataUtil.getTag(loaded).getStringOr("test:preserved", "")).isEqualTo("kept");
      assertThat(ToolStack.from(stack).getStats().get(ToolStats.DURABILITY)).isEqualTo(25f);
    } finally {
      definition.setData(oldDefinition);
      ModifierManager.INSTANCE.dynamicModifiersLoaded = oldLoaded;
    }
  }

  @Test
  void materialDataUnavailablePreservesSavedPart() {
    ItemStack stack = MaterialItemFixture.MATERIAL_ITEM.withMaterialForDisplay(new MaterialId("test", "old_material"));
    try (var materials = mockStatic(MaterialRegistry.class)) {
      materials.when(MaterialRegistry::isFullyLoaded).thenReturn(false);
      assertThat(ItemStack.isSameItemSameComponents(reload(stack), stack)).isTrue();
      materials.verify(MaterialRegistry::getInstance, never());
    }
  }
}

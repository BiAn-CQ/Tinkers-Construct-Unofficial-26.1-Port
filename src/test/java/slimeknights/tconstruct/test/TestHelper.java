package slimeknights.tconstruct.test;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionData;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Helpers for running tests */
public class TestHelper {
  private TestHelper() {}

  /** Evaluates one registered item's delayed defaults without binding the whole unit-test registry. */
  public static DataComponentMap defaultComponents(Item item) throws ReflectiveOperationException {
    ResourceKey<Item> itemKey = BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    Field entriesField = DataComponentInitializers.class.getDeclaredField("initializers");
    entriesField.setAccessible(true);
    List<?> entries = (List<?>)entriesField.get(BuiltInRegistries.DATA_COMPONENT_INITIALIZERS);
    DataComponentMap.Builder components = DataComponentMap.builder();
    HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    for (Object entry : entries) {
      Method keyMethod = entry.getClass().getDeclaredMethod("key");
      keyMethod.setAccessible(true);
      if (itemKey.equals(keyMethod.invoke(entry))) {
        Method runMethod = entry.getClass().getDeclaredMethod("run", DataComponentMap.Builder.class, HolderLookup.Provider.class);
        runMethod.setAccessible(true);
        runMethod.invoke(entry, components, registries);
      }
    }
    return components.build();
  }

  /** Helper to fetch traits from the trait hook */
  public static List<ModifierEntry> getTraits(ToolDefinitionData data) {
    ModifierNBT.Builder builder = ModifierNBT.builder();
    data.getHook(ToolHooks.TOOL_TRAITS).addTraits(ToolDefinition.EMPTY, MaterialNBT.EMPTY, builder);
    return builder.build().getModifiers();
  }

  public record ToolDefinitionStats(StatsNBT base, MultiplierNBT multipliers) {}

  /** Computes the stats for the given tool */
  public static ToolDefinitionStats buildStats(ToolDefinitionData data) {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    data.getHook(ToolHooks.TOOL_STATS).addToolStats(new DummyToolStack(Items.AIR, ModifierNBT.EMPTY, new ModDataNBT()), builder);
    MultiplierNBT multipliers = builder.buildMultipliers();
    // cancel out multipliers on the base stats, as people expect base stats to be comparable to be usable in the modifier stats builder
    for (INumericToolStat<?> stat : multipliers.getContainedStats()) {
      stat.multiply(builder, 1 / multipliers.get(stat));
    }
    return new ToolDefinitionStats(builder.build(), multipliers);
  }
}

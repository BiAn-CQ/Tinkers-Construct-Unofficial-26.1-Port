package slimeknights.tconstruct.library.json.loot;

import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;
import java.util.function.Consumer;

/** Loot entry that resolves an optional compat item by registry name when the loot is generated. */
public final class ItemNameLootEntry extends LootPoolSingletonContainer {
  public static final MapCodec<ItemNameLootEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Identifier.CODEC.fieldOf("name").forGetter(entry -> entry.name)
  ).and(singletonFields(instance)).apply(instance, ItemNameLootEntry::new));

  private final Identifier name;

  @Override
  public MapCodec<? extends LootPoolSingletonContainer> codec() {
    return CODEC;
  }

  private ItemNameLootEntry(Identifier name, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
    super(weight, quality, conditions, functions);
    this.name = name;
  }


  @Override
  protected void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
    Item item = BuiltInRegistries.ITEM.getOptional(name)
      .orElseThrow(() -> new IllegalStateException("Missing optional loot item " + name));
    consumer.accept(new ItemStack(item));
  }

  public static LootPoolSingletonContainer.Builder<?> item(Identifier name) {
    return simpleBuilder((weight, quality, conditions, functions) -> new ItemNameLootEntry(name, weight, quality, conditions, functions));
  }
}

package slimeknights.tconstruct.smeltery.block.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TankDropTest extends CoreTestBootstrap {
  @Test
  void minedTanksRetainFluidAndComponentsWhenPlacedAgain() throws Exception {
    FluidStack fluid = new FluidStack(Fluids.LAVA, 2375);
    fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Stored fluid"));

    for (Block block : tankBlocks()) {
      BlockEntity source = newBlockEntity(block);
      int amount = Math.min(fluid.getAmount(), ((ITankBlockEntity)source).getTank().getCapacity());
      FluidStack expected = fluid.copyWithAmount(amount);
      ((ITankBlockEntity)source).getTank().setFluid(expected.copy());

      ItemStack dropped = drop(block, source);
      assertFluid(TankItem.getTank(dropped, 1).getFluid(), expected);
      assertThat(dropped.has(DataComponents.CUSTOM_DATA)).as("legacy tank data for %s", block).isFalse();

      // Copying the block entity must not leave the item sharing its mutable fluid stack.
      ((ITankBlockEntity)source).getTank().setFluid(FluidStack.EMPTY);
      assertFluid(TankItem.getTank(dropped, 1).getFluid(), expected);

      BlockEntity placed = newBlockEntity(block);
      placed.applyComponentsFromItemStack(dropped);
      assertFluid(((ITankBlockEntity)placed).getTank().getFluid(), expected);
      assertThat(placed.components().has(TinkerModule.FLUID_STACK_COMPONENT.get())).isFalse();
    }
  }

  @Test
  void emptiedTanksDoNotDropStaleFluidComponents() throws Exception {
    FluidStack fluid = new FluidStack(Fluids.WATER, 1000);
    for (Block block : tankBlocks()) {
      BlockEntity source = newBlockEntity(block);
      // Old placements retained an unused fluid component in the block entity.
      source.setComponents(DataComponentMap.builder()
        .set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(fluid)).build());
      ((ITankBlockEntity)source).getTank().setFluid(FluidStack.EMPTY);

      ItemStack dropped = drop(block, source);
      assertThat(dropped.has(TinkerModule.FLUID_STACK_COMPONENT.get())).as("empty tank %s", block).isFalse();
      assertThat(TankItem.getTank(dropped, 1).isEmpty()).isTrue();

      BlockEntity placed = newBlockEntity(block);
      placed.applyComponentsFromItemStack(dropped);
      assertThat(((ITankBlockEntity)placed).getTank().isEmpty()).isTrue();
    }
  }

  private static List<Block> tankBlocks() {
    List<Block> blocks = new ArrayList<>();
    TinkerSmeltery.searedTank.forEach(block -> blocks.add(block));
    TinkerSmeltery.scorchedTank.forEach(block -> blocks.add(block));
    blocks.add(TinkerSmeltery.searedFluidCannon.get());
    blocks.add(TinkerSmeltery.scorchedFluidCannon.get());
    blocks.add(TinkerSmeltery.endFluidCannon.get());
    blocks.add(TinkerSmeltery.searedLantern.get());
    blocks.add(TinkerSmeltery.scorchedLantern.get());
    blocks.add(TinkerSmeltery.searedCastingTank.get());
    return blocks;
  }

  private static BlockEntity newBlockEntity(Block block) {
    return ((EntityBlock)block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
  }

  /** Executes the actual packaged loot functions against a real tank block entity. */
  private static ItemStack drop(Block block, BlockEntity source) throws Exception {
    RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    ServerLevel level = mock(ServerLevel.class);
    when(level.registryAccess()).thenReturn(registries);
    source.setLevel(level);
    LootContext context = mock(LootContext.class);
    when(context.getLevel()).thenReturn(level);
    when(context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)).thenReturn(source);

    String path = "/data/tconstruct/loot_table/blocks/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + ".json";
    try (InputStream stream = TankDropTest.class.getResourceAsStream(path)) {
      assertThat(stream).as("loot table %s", path).isNotNull();
      JsonArray functions = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        .getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject()
        .getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions");
      ItemStack dropped = new ItemStack(block);
      for (var json : functions) {
        LootItemFunction function = LootItemFunctions.ROOT_CODEC
          .parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
        dropped = function.apply(dropped, context);
      }
      return dropped;
    }
  }

  private static void assertFluid(FluidStack actual, FluidStack expected) {
    assertThat(actual.isEmpty()).isFalse();
    assertThat(FluidStack.isSameFluidSameComponents(actual, expected)).isTrue();
    assertThat(actual.getAmount()).isEqualTo(expected.getAmount());
  }
}

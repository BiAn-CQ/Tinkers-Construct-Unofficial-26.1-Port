package slimeknights.tconstruct.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinkerHeadItemTest {
  @Test
  void allMobHeadsCanBeEquippedInHeadSlot() throws ReflectiveOperationException {
    for (TinkerHeadType type : TinkerHeadType.values()) {
      Equippable equippable = defaultComponents(TinkerWorld.headItems.get(type)).get(DataComponents.EQUIPPABLE);

      assertThat(equippable).as("equippable component for %s", type).isNotNull();
      assertThat(equippable.slot()).as("equipment slot for %s", type).isEqualTo(EquipmentSlot.HEAD);
      assertThat(equippable.swappable()).as("right-click swapping for %s", type).isFalse();
      assertThat(equippable.dispensable()).as("dispenser equipping for %s", type).isTrue();
    }
  }

  @Test
  void dispenserEquipsOneHeadUsingNativeEquipmentRules() {
    BlockSource source = mock(BlockSource.class);
    ServerLevel level = mock(ServerLevel.class);
    BlockState dispenserState = mock(BlockState.class);
    LivingEntity target = mock(LivingEntity.class);
    ItemStack stack = mock(ItemStack.class);
    ItemStack equipped = mock(ItemStack.class);

    when(source.level()).thenReturn(level);
    when(source.pos()).thenReturn(BlockPos.ZERO);
    when(source.state()).thenReturn(dispenserState);
    when(dispenserState.getValue(DispenserBlock.FACING)).thenReturn(Direction.NORTH);
    when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenReturn(List.of(target));
    when(target.getEquipmentSlotForItem(stack)).thenReturn(EquipmentSlot.HEAD);
    when(stack.canEquip(EquipmentSlot.HEAD, target)).thenReturn(true);
    when(stack.split(1)).thenReturn(equipped);

    assertThat(TinkerWorld.dispenseHead(source, stack)).isTrue();
    verify(stack).split(1);
    verify(target).setItemSlot(EquipmentSlot.HEAD, equipped);
  }

  /** Evaluates one registered item's delayed defaults without binding the whole unit-test registry. */
  private static DataComponentMap defaultComponents(Item item) throws ReflectiveOperationException {
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
}

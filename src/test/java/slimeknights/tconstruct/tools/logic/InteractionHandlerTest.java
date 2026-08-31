package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteractionHandlerTest {
  @Test
  void forwardsTheActualLocalHitForEitherHand() {
    for (InteractionHand hand : InteractionHand.values()) {
      Entity target = mock(Entity.class);
      Fixture fixture = new Fixture(target, hand, new Vec3(0.2, 1.25, -0.1));
      when(target.interact(fixture.player, hand, fixture.hit)).thenReturn(InteractionResult.SUCCESS_SERVER);
      try (MockedStatic<CommonHooks> hooks = mockStatic(CommonHooks.class)) {
        InteractionHandler.afterEntityInteract(fixture.event);
        hooks.verify(() -> CommonHooks.onInteractEntity(fixture.player, target, hand));
      }
      verify(target).interact(fixture.player, hand, fixture.hit);
      verify(fixture.event).setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }
  }

  @Test
  void vanillaArmorStandRemovesTheClickedArmorSlotAtWorldHeight() {
    Map<EquipmentSlot,Double> heights = Map.of(
      EquipmentSlot.FEET, 0.2, EquipmentSlot.LEGS, 0.7,
      EquipmentSlot.CHEST, 1.25, EquipmentSlot.HEAD, 1.8);
    for (var clicked : heights.entrySet()) {
      ArmorStand stand = mock(ArmorStand.class);
      Fixture fixture = new Fixture(stand, InteractionHand.MAIN_HAND, new Vec3(0, clicked.getValue(), 0));
      // Use vanilla's actual hit-region selection and item swapping, with an isolated inventory.
      when(fixture.player.level()).thenReturn(mock(Level.class));
      when(fixture.player.getItemInHand(fixture.hand)).thenReturn(ItemStack.EMPTY);
      when(stand.position()).thenReturn(new Vec3(-130, 70, -402));
      when(stand.getScale()).thenReturn(1f);
      when(stand.getAgeScale()).thenReturn(1f);
      when(stand.getEquipmentSlotForItem(ItemStack.EMPTY)).thenReturn(EquipmentSlot.MAINHAND);
      Map<EquipmentSlot,ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
      heights.keySet().forEach(slot -> equipment.put(slot, mock(ItemStack.class)));
      ItemStack expected = equipment.get(clicked.getKey());
      when(stand.hasItemInSlot(any())).thenAnswer(call -> equipment.containsKey(call.getArgument(0)));
      when(stand.getItemBySlot(any())).thenAnswer(call -> equipment.getOrDefault(call.getArgument(0), ItemStack.EMPTY));
      doAnswer(call -> { equipment.put(call.getArgument(0), call.getArgument(1)); return null; })
        .when(stand).setItemSlot(any(), any());
      doCallRealMethod().when(stand).interact(fixture.player, fixture.hand, fixture.hit);
      try (MockedStatic<CommonHooks> hooks = mockStatic(CommonHooks.class)) {
        InteractionHandler.afterEntityInteract(fixture.event);
      }
      verify(stand).setItemSlot(clicked.getKey(), ItemStack.EMPTY);
      verify(fixture.player).setItemInHand(fixture.hand, expected);
      verify(fixture.event).setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }
  }

  @Test
  void preservesGeneralEventCancellationsIncludingPass() {
    for (InteractionResult cancellation : List.of(InteractionResult.PASS, InteractionResult.FAIL, InteractionResult.SUCCESS)) {
      Entity target = mock(Entity.class);
      Fixture fixture = new Fixture(target, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
      try (MockedStatic<CommonHooks> hooks = mockStatic(CommonHooks.class)) {
        hooks.when(() -> CommonHooks.onInteractEntity(fixture.player, target, fixture.hand)).thenReturn(cancellation);
        InteractionHandler.afterEntityInteract(fixture.event);
        hooks.verify(() -> CommonHooks.onInteractEntity(fixture.player, target, fixture.hand));
      }
      verify(target, never()).interact(any(), any(), any());
      verify(fixture.event).setCancellationResult(cancellation);
    }
  }

  @Test
  void preservesUpstreamBeforeEntityAfterAndGeneralHookOrder() {
    LivingEntity target = mock(LivingEntity.class);
    Fixture fixture = new Fixture(target, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
    ToolStack tool = mock(ToolStack.class);
    ModifierEntry modifier = mock(ModifierEntry.class);
    EntityInteractionModifierHook entityHook = mock(EntityInteractionModifierHook.class);
    GeneralInteractionModifierHook useHook = mock(GeneralInteractionModifierHook.class);
    when(tool.getModifierList()).thenReturn(List.of(modifier));
    when(modifier.getHook(ModifierHooks.ENTITY_INTERACT)).thenReturn(entityHook);
    when(modifier.getHook(ModifierHooks.GENERAL_INTERACT)).thenReturn(useHook);
    when(target.blockPosition()).thenReturn(BlockPos.ZERO);
    when(target.interact(fixture.player, fixture.hand, fixture.hit)).thenReturn(InteractionResult.PASS);
    when(entityHook.beforeEntityUse(tool, modifier, fixture.player, target, fixture.hand, InteractionSource.ARMOR))
      .thenReturn(InteractionResult.PASS);
    when(entityHook.afterEntityUse(tool, modifier, fixture.player, target, fixture.hand, InteractionSource.ARMOR))
      .thenReturn(InteractionResult.PASS);
    when(useHook.onToolUse(tool, modifier, fixture.player, fixture.hand, InteractionSource.ARMOR))
      .thenReturn(InteractionResult.SUCCESS);
    try (MockedStatic<CommonHooks> hooks = mockStatic(CommonHooks.class);
         MockedStatic<ToolStack> tools = mockStatic(ToolStack.class)) {
      tools.when(() -> ToolStack.from(fixture.chestplate)).thenReturn(tool);
      hooks.when(() -> CommonHooks.onInteractEntity(fixture.player, target, fixture.hand)).thenAnswer(call -> {
        EntityInteract generalEvent = new EntityInteract(fixture.player, fixture.hand, target);
        InteractionHandler.beforeEntityInteract(generalEvent);
        return generalEvent.isCanceled() ? generalEvent.getCancellationResult() : null;
      });
      InteractionHandler.afterEntityInteract(fixture.event);
    }
    var order = inOrder(target, entityHook, useHook);
    order.verify(entityHook).beforeEntityUse(tool, modifier, fixture.player, target, fixture.hand, InteractionSource.ARMOR);
    order.verify(target).interact(fixture.player, fixture.hand, fixture.hit);
    order.verify(entityHook).afterEntityUse(tool, modifier, fixture.player, target, fixture.hand, InteractionSource.ARMOR);
    order.verify(useHook).onToolUse(tool, modifier, fixture.player, fixture.hand, InteractionSource.ARMOR);
    verify(fixture.event).setCancellationResult(InteractionResult.SUCCESS);
  }

  @Test
  void leavesOrdinaryChestplatesAndOccupiedHandsToVanilla() {
    Entity target = mock(Entity.class);
    Fixture fixture = new Fixture(target, InteractionHand.MAIN_HAND, Vec3.ZERO);
    when(fixture.chestplate.is(TinkerTags.Items.INTERACTABLE_ARMOR)).thenReturn(false);
    InteractionHandler.afterEntityInteract(fixture.event);
    when(fixture.chestplate.is(TinkerTags.Items.INTERACTABLE_ARMOR)).thenReturn(true);
    when(fixture.event.getItemStack()).thenReturn(mock(ItemStack.class));
    InteractionHandler.afterEntityInteract(fixture.event);
    verify(fixture.event, never()).setCanceled(true);
    verify(target, never()).interact(any(), any(), any());
  }

  private static class Fixture {
    private final Player player = mock(Player.class);
    private final ItemStack chestplate = mock(ItemStack.class);
    private final EntityInteractSpecific event = mock(EntityInteractSpecific.class);
    private final InteractionHand hand;
    private final Vec3 hit;

    private Fixture(Entity target, InteractionHand hand, Vec3 hit) {
      this.hand = hand;
      this.hit = hit;
      when(event.getEntity()).thenReturn(player);
      when(event.getTarget()).thenReturn(target);
      when(event.getHand()).thenReturn(hand);
      when(event.getLocalPos()).thenReturn(hit);
      when(event.getItemStack()).thenReturn(ItemStack.EMPTY);
      when(player.getItemInHand(hand)).thenReturn(ItemStack.EMPTY);
      when(player.getItemBySlot(EquipmentSlot.CHEST)).thenReturn(chestplate);
      when(player.getCooldowns()).thenReturn(mock(ItemCooldowns.class));
      when(chestplate.is(TinkerTags.Items.INTERACTABLE_ARMOR)).thenReturn(true);
    }
  }
}

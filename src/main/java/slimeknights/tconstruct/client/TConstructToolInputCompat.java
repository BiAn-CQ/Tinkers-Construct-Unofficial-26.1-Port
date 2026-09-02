package slimeknights.tconstruct.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.tools.logic.DoubleJumpHandler;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.network.TinkerControlPacket;

/**
 * 26.1 replacement for the input-only portion of {@code ToolClientEvents}.
 * Model, renderer, and tint registrations live in their native compatibility
 * classes; keeping input separate prevents the removed baking API from being
 * pulled back into the runtime artifact.
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructToolInputCompat {
  private static final KeyMapping.Category CATEGORY =
    new KeyMapping.Category(TConstruct.getResource("tconstruct"));
  private static final KeyMapping HELMET_INTERACT = new KeyMapping(
    TConstruct.makeTranslationKey("key", "helmet_interact"),
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
  private static final KeyMapping LEGGINGS_INTERACT = new KeyMapping(
    TConstruct.makeTranslationKey("key", "leggings_interact"),
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

  private static boolean wasJumping;
  private static boolean wasHelmetInteracting;
  private static boolean wasLeggingsInteracting;

  private TConstructToolInputCompat() {}

  @SubscribeEvent
  static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    event.registerCategory(CATEGORY);
    event.register(HELMET_INTERACT);
    event.register(LEGGINGS_INTERACT);
  }

  /** Handles edge-triggered jumps and press/release armor interactions. */
  @SubscribeEvent
  static void handleKeyBindings(PlayerTickEvent.Pre event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.player != event.getEntity() || minecraft.player.isSpectator()) {
      return;
    }

    boolean jumping = minecraft.options.keyJump.isDown();
    if (!wasJumping && jumping) {
      if (TinkerEffects.antigravity.get().antigravityJump(event.getEntity())) {
        TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.ANTIGRAVITY_JUMP);
      } else if (DoubleJumpHandler.extraJump(event.getEntity())) {
        TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.DOUBLE_JUMP);
      }
    }
    wasJumping = jumping;

    boolean helmetInteracting = HELMET_INTERACT.isDown();
    if (!wasHelmetInteracting && helmetInteracting) {
      TooltipKey key = SafeClientAccess.getTooltipKey();
      if (InteractionHandler.startArmorInteract(event.getEntity(), EquipmentSlot.HEAD, key)) {
        TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartHelmetInteract(key));
      }
    } else if (wasHelmetInteracting && !helmetInteracting
               && InteractionHandler.stopArmorInteract(event.getEntity(), EquipmentSlot.HEAD)) {
      TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_HELMET_INTERACT);
    }
    wasHelmetInteracting = helmetInteracting;

    boolean leggingsInteracting = LEGGINGS_INTERACT.isDown();
    if (!wasLeggingsInteracting && leggingsInteracting) {
      TooltipKey key = SafeClientAccess.getTooltipKey();
      if (InteractionHandler.startArmorInteract(event.getEntity(), EquipmentSlot.LEGS, key)) {
        TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartLeggingsInteract(key));
      }
    } else if (wasLeggingsInteracting && !leggingsInteracting
               && InteractionHandler.stopArmorInteract(event.getEntity(), EquipmentSlot.LEGS)) {
      TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_LEGGINGS_INTERACT);
    }
    wasLeggingsInteracting = leggingsInteracting;
  }

  /**
   * Restores the tool and armor item-use movement stats. In 26.1 vanilla applies the
   * held item's {@link UseEffects#speedMultiplier()} after this event, so the input
   * is scaled relative to that component instead of assuming the old fixed 20% speed.
   */
  @SuppressWarnings("removal")
  @SubscribeEvent
  static void handleMovementInput(MovementInputUpdateEvent event) {
    Player player = event.getEntity();
    if (!player.isUsingItem() || player.isPassenger()) {
      return;
    }

    ItemStack using = player.getUseItem();
    double speed = player.getAttributeValue(TinkerAttributes.USE_ITEM_SPEED);
    if (using.is(TinkerTags.Items.HELD)) {
      ToolStack tool = ToolStack.from(using);
      speed += tool.getStats().get(ToolStats.USE_ITEM_SPEED) - ToolStats.USE_ITEM_SPEED.getDefaultValue();
    }
    speed = Mth.clamp(speed, 0, 1);

    ClientInput input = event.getInput();
    Vec2 movement = input.getMoveVector();
    float vanillaMultiplier = using.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
    if (vanillaMultiplier > 0) {
      input.moveVector = movement.scale((float)(speed / vanillaMultiplier));
    }
  }

  @SubscribeEvent
  static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    wasJumping = false;
    wasHelmetInteracting = false;
    wasLeggingsInteracting = false;
  }
}

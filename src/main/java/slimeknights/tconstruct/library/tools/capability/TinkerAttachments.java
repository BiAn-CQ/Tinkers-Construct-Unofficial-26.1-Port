package slimeknights.tconstruct.library.tools.capability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.capability.PiggybackHandler;
import slimeknights.tconstruct.tools.logic.EquipmentChangeWatcher;

/** Native NeoForge data attachments used for serializable per-entity tool data. */
public final class TinkerAttachments {
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  public static final DeferredHolder<AttachmentType<?>,AttachmentType<EntityModifierCapability.Data>> ENTITY_MODIFIERS =
    ATTACHMENTS.register("modifiers", () -> AttachmentType.serializable(EntityModifierCapability.Data::new).build());
  public static final DeferredHolder<AttachmentType<?>,AttachmentType<PersistentDataCapability.Data>> PERSISTENT_DATA =
    ATTACHMENTS.register("persistent_data", () -> AttachmentType.serializable(PersistentDataCapability.Data::new).copyOnDeath().build());
  public static final DeferredHolder<AttachmentType<?>,AttachmentType<TinkerDataCapability.Holder>> TINKER_DATA =
    ATTACHMENTS.register("modifier_data", () -> AttachmentType.builder(TinkerDataCapability.Holder::new).build());
  public static final DeferredHolder<AttachmentType<?>,AttachmentType<PiggybackHandler>> PIGGYBACK =
    ATTACHMENTS.register("piggyback", () -> AttachmentType.builder(holder -> new PiggybackHandler((net.minecraft.world.entity.player.Player)holder)).build());
  public static final DeferredHolder<AttachmentType<?>,AttachmentType<EquipmentChangeWatcher.PlayerLastEquipment>> EQUIPMENT_WATCHER =
    ATTACHMENTS.register("equipment_watcher", () -> AttachmentType.builder(holder -> new EquipmentChangeWatcher.PlayerLastEquipment((net.minecraft.world.entity.player.Player)holder)).build());

  private TinkerAttachments() {}

  public static void register(IEventBus bus) {
    ATTACHMENTS.register(bus);
  }
}

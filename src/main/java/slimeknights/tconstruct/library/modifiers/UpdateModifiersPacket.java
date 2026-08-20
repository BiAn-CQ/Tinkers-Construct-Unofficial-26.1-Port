package slimeknights.tconstruct.library.modifiers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.impl.ComposableModifier;
import slimeknights.tconstruct.library.utils.GenericTagUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Packet to sync modifiers */
@RequiredArgsConstructor
public class UpdateModifiersPacket implements IThreadsafePacket {
  /** Collection of all modifiers */
  private final Map<ModifierId,Modifier> allModifiers;
  /** Map of all modifier tags */
  private final Map<TagKey<Modifier>,List<Modifier>> tags;
  /** Collection of non-redirect modifiers */
  private Collection<ComposableModifier> modifiers;
  /** Map of modifier redirect ID pairs */
  private Map<ModifierId,ModifierId> redirects;
  /** Map of enchantment to modifier pair */
  private final Map<Holder<Enchantment>,Modifier> enchantmentMap;
  /** Collection of all enchantment tag mappings */
  private final Map<HolderSet.Named<Enchantment>, Modifier> enchantmentTagMappings;

  /** Ensures both the modifiers and redirects lists are calculated, allows one packet to be used multiple times without redundant work */
  private void ensureCalculated() {
    if (this.modifiers == null || this.redirects == null) {
      ImmutableList.Builder<ComposableModifier> modifiers = ImmutableList.builder();
      ImmutableMap.Builder<ModifierId,ModifierId> redirects = ImmutableMap.builder();
      for (Entry<ModifierId,Modifier> entry : allModifiers.entrySet()) {
        ModifierId id = entry.getKey();
        Modifier value = entry.getValue();
        ModifierId actual = value.getId();
        if (id.equals(actual)) {
          // we can't sync anything that is not composable
          if (value instanceof ComposableModifier composable) {
            modifiers.add(composable);
          } else {
            TConstruct.LOG.warn("Unable to sync modifier {} as its not ComposableModifier; got class {}", id, value.getClass().getName());
          }
        } else {
          redirects.put(id, actual);
        }
      }
      this.modifiers = modifiers.build();
      this.redirects = redirects.build();
    }
  }

  /** Gets a modifier by the given ID, falling back to the map if needed */
  private static Modifier getModifier(Map<ModifierId,Modifier> modifiers, ModifierId id) {
    Modifier modifier = ModifierManager.INSTANCE.getStatic(id);
    if (modifier == ModifierManager.INSTANCE.getDefaultValue()) {
      modifier = modifiers.get(id);
      if (modifier == null) {
        throw new DecoderException("Unknown modifier " + id);
      }
    }
    return modifier;
  }

  public UpdateModifiersPacket(FriendlyByteBuf buffer) {
    if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
      throw new DecoderException("Modifier synchronization requires registry-aware network context");
    }
    var enchantmentRegistry = registryBuffer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    // read in modifiers
    int size = buffer.readVarInt();
    Map<ModifierId,Modifier> modifiers = new HashMap<>();
    for (int i = 0; i < size; i++) {
      ModifierId id = new ModifierId(buffer.readUtf(Short.MAX_VALUE));
      try {
        Modifier modifier = ComposableModifier.LOADER.decode(buffer, ModifierManager.contextBuilder(id.location(), registryBuffer.registryAccess()).build());
        modifier.setId(id);
        modifiers.put(id, modifier);
      } catch (RuntimeException e) {
        TConstruct.LOG.error("Failed to decode modifier with ID {}", id, e);
        throw e;
      }
    }
    // read in redirects
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      ModifierId from = new ModifierId(buffer.readUtf(Short.MAX_VALUE));
      modifiers.put(from, getModifier(modifiers, new ModifierId(buffer.readUtf(Short.MAX_VALUE))));
    }
    this.allModifiers = modifiers;
    this.tags = GenericTagUtil.decodeTags(buffer, ModifierManager.REGISTRY_KEY, id -> getModifier(modifiers, new ModifierId(id)));

    // read in enchantment to modifier mapping
    ImmutableMap.Builder<Holder<Enchantment>,Modifier> enchantmentBuilder = ImmutableMap.builder();
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      enchantmentBuilder.put(
        enchantmentRegistry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, buffer.readIdentifier())),
        getModifier(modifiers, new ModifierId(buffer.readIdentifier())));
    }
    enchantmentMap = enchantmentBuilder.build();
    ImmutableMap.Builder<HolderSet.Named<Enchantment>, Modifier> enchantmentTagBuilder = ImmutableMap.builder();
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      enchantmentTagBuilder.put(
        enchantmentRegistry.get(TagKey.create(Registries.ENCHANTMENT, buffer.readIdentifier()))
          .orElseThrow(() -> new DecoderException("Unknown enchantment tag in modifier synchronization")),
        getModifier(modifiers, new ModifierId(buffer.readIdentifier())));
    }
    enchantmentTagMappings = enchantmentTagBuilder.build();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    ensureCalculated();
    // write modifiers
    buffer.writeVarInt(modifiers.size());
    for (ComposableModifier modifier : modifiers) {
      Identifier id = modifier.getId().location();
      buffer.writeIdentifier(id);
      try {
        ComposableModifier.LOADER.encode(buffer, modifier);
      } catch (RuntimeException e) {
        // improve error logging
        TConstruct.LOG.error("Failed to encode modifier with ID {}", id, e);
        throw e;
      }
    }
    // write redirects
    buffer.writeVarInt(redirects.size());
    for (Entry<ModifierId,ModifierId> entry : redirects.entrySet()) {
      buffer.writeIdentifier(entry.getKey().location());
      buffer.writeIdentifier(entry.getValue().location());
    }
    GenericTagUtil.encodeTags(buffer, Modifier::getId, ModifierId::location, this.tags);

    // enchantment mapping
    buffer.writeVarInt(enchantmentMap.size());
    for (Entry<Holder<Enchantment>,Modifier> entry : enchantmentMap.entrySet()) {
      buffer.writeIdentifier(entry.getKey().unwrapKey().orElseThrow().identifier());
      buffer.writeIdentifier(entry.getValue().getId().location());
    }
    buffer.writeVarInt(enchantmentTagMappings.size());
    for (Entry<HolderSet.Named<Enchantment>, Modifier> entry : enchantmentTagMappings.entrySet()) {
      buffer.writeIdentifier(entry.getKey().key().location());
      buffer.writeIdentifier(entry.getValue().getId().location());
    }
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    ModifierManager.INSTANCE.updateModifiersFromServer(allModifiers, tags, enchantmentMap, enchantmentTagMappings);
  }
}

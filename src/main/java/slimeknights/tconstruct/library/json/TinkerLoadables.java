package slimeknights.tconstruct.library.json;

import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import slimeknights.tconstruct.library.compat.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Instrument;
import slimeknights.tconstruct.library.compat.Tier;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import slimeknights.tconstruct.library.utils.TierRegistry;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.RegistryLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialManager;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.Set;

@SuppressWarnings("deprecation")
public class TinkerLoadables {
  /* Enums */
  /** Attribute operation loadable accepting the 1.20.1 names during migration. */
  public static final StringLoadable<Operation> OPERATION = new StringLoadable<>() {
    @Override
    public Operation parseString(String name, String key, TypedMap context) {
      return switch (name) {
        case "add", "addition", "add_value" -> Operation.ADD_VALUE;
        case "multiply_base", "add_multiplied_base" -> Operation.ADD_MULTIPLIED_BASE;
        case "multiply_total", "add_multiplied_total" -> Operation.ADD_MULTIPLIED_TOTAL;
        default -> throw new JsonSyntaxException("Invalid Operation " + name);
      };
    }

    @Override
    public String getString(Operation object) {
      return switch (object) {
        case ADD_VALUE -> "add_value";
        case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
        case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
      };
    }

    @Override
    public Operation decode(FriendlyByteBuf buffer, TypedMap context) {
      return buffer.readEnum(Operation.class);
    }

    @Override
    public void encode(FriendlyByteBuf buffer, Operation object) {
      buffer.writeEnum(object);
    }
  };
  public static final StringLoadable<EquipmentSlot> EQUIPMENT_SLOT = new EnumLoadable<>(EquipmentSlot.class);
  public static final Loadable<Set<EquipmentSlot>> EQUIPMENT_SLOT_SET = EQUIPMENT_SLOT.set();
  public static final StringLoadable<ArmorItem.Type> ARMOR_SLOT = new EnumLoadable<>(ArmorItem.Type.class);
  public static final StringLoadable<LightLayer> LIGHT_LAYER = new EnumLoadable<>(LightLayer.class);
  public static final StringLoadable<InteractionSource> INTERACTION_SOURCE = new EnumLoadable<>(InteractionSource.class);
  public static final StringLoadable<OreRateType> ORE_RATE_TYPE = new EnumLoadable<>(OreRateType.class);
  public static final StringLoadable<TooltipKey> TOOLTIP_KEY = new EnumLoadable<>(TooltipKey.class);

  /* Registries */
  public static final StringLoadable<StatType<?>> STAT_TYPE = new RegistryLoadable<>(BuiltInRegistries.STAT_TYPE);
  public static final StringLoadable<Identifier> CUSTOM_STAT = new RegistryLoadable<>(BuiltInRegistries.CUSTOM_STAT);
  public static final StringLoadable<RecipeType<?>> RECIPE_TYPE = new RegistryLoadable<>(BuiltInRegistries.RECIPE_TYPE);
  /** Holder-valued enchantment loadable for registries that require holder identity. */
  public static final StringLoadable<Holder<Enchantment>> ENCHANTMENT_HOLDER = new StringLoadable<>() {
    @Override
    public Holder<Enchantment> parseString(String name, String key, TypedMap context) {
      RegistryOps.RegistryInfoLookup registryLookup = context.get(ContextKey.REGISTRY_LOOKUP);
      if (registryLookup != null) {
        ResourceKey<Enchantment> enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(name));
        RegistryOps.RegistryInfo<Enchantment> info = registryLookup.lookup(Registries.ENCHANTMENT).orElse(null);
        if (info != null) {
          return info.getter().getOrThrow(enchantmentKey);
        }
      }
      RegistryAccess access = context.get(ContextKey.REGISTRY_ACCESS);
      if (access != null) {
        ResourceKey<Enchantment> enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(name));
        return access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
      }
      Enchantment enchantment = Loadables.ENCHANTMENT.parseString(name, key, context);
      Registry<Enchantment> registry = RegistryHelper.getRegistry(Registries.ENCHANTMENT);
      if (registry == null) {
        throw new IllegalStateException("Enchantment registry is not available");
      }
      return registry.wrapAsHolder(enchantment);
    }

    @Override
    public String getString(Holder<Enchantment> holder) {
      return holder.unwrapKey().map(resourceKey -> resourceKey.identifier().toString()).orElseGet(() -> {
        Registry<Enchantment> registry = RegistryHelper.getRegistry(Registries.ENCHANTMENT);
        if (registry == null) {
          throw new IllegalStateException("Enchantment registry is not available");
        }
        Identifier id = registry.getKey(holder.value());
        if (id == null) {
          throw new IllegalArgumentException("Unknown enchantment " + holder.value());
        }
        return id.toString();
      });
    }

    @Override
    public Holder<Enchantment> decode(FriendlyByteBuf buffer, TypedMap context) {
      int id = buffer.readVarInt();
      Registry<Enchantment> registry = context.get(ContextKey.REGISTRY_ACCESS) != null
        ? context.get(ContextKey.REGISTRY_ACCESS).lookupOrThrow(Registries.ENCHANTMENT)
        : RegistryHelper.getRegistry(Registries.ENCHANTMENT);
      if (registry == null) {
        throw new IllegalStateException("Enchantment registry is not available");
      }
      return registry.get(id).orElseThrow(() -> new IllegalArgumentException("Unknown enchantment registry ID " + id));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, Holder<Enchantment> holder) {
      Registry<Enchantment> registry = RegistryHelper.getRegistry(Registries.ENCHANTMENT);
      if (registry == null) {
        throw new IllegalStateException("Enchantment registry is not available");
      }
      buffer.writeVarInt(registry.getId(holder.value()));
    }
  };

  /* Tag keys */
  public static final StringLoadable<TagKey<Modifier>> MODIFIER_TAGS = Loadables.tagKey(ModifierManager.REGISTRY_KEY);
  public static final StringLoadable<TagKey<IMaterial>> MATERIAL_TAGS = Loadables.tagKey(MaterialManager.REGISTRY_KEY);
  public static final StringLoadable<TagKey<Instrument>> INSTRUMENT_TAGS = Loadables.tagKey(Registries.INSTRUMENT);

  /* Mapped items */
  public static final StringLoadable<IMaterialItem> MATERIAL_ITEM = instance(Loadables.ITEM, IMaterialItem.class, "Expected item to be instance of IMaterialItem");
  public static final StringLoadable<IModifiable> MODIFIABLE_ITEM = instance(Loadables.ITEM, IModifiable.class, "Expected item to be instance of IModifiable");
  public static final StringLoadable<IToolPart> TOOL_PART_ITEM = instance(Loadables.ITEM, IToolPart.class, "Expected item to be instance of IToolPart");
  public static final StringLoadable<SimpleParticleType> SIMPLE_PARTICLE = instance(Loadables.PARTICLE_TYPE, SimpleParticleType.class, "Expected particle type to be instance of SimpleParticleType");
  public static final StringLoadable<BlockItem> BLOCK_ITEM = instance(Loadables.ITEM, BlockItem.class, "Expected item to be instance of BlockItem");

  /** Tier loadable from the forge tier sorting registry */
  public static final StringLoadable<Tier> TIER = Loadables.RESOURCE_LOCATION.xmap((id, error) -> {
    Tier tier = TierRegistry.byName(id);
    if (tier != null) {
      return tier;
    }
    throw error.create("Unknown harvest tier " + id);
  }, (tier, error) -> {
    Identifier id = TierRegistry.getName(tier);
    if (id != null) {
      return id;
    }
    throw error.create("Attempt to serialize unregistered tier " + tier);
  });

  /* Loot tables */
  /** Loadable for a loot entry instance */
  public static final Loadable<LootPoolEntryContainer> LOOT_ENTRY = Loadables.LOOT_ENTRY;

  /** Loadble requiring the argument to be an instance of the passed class */
  @SuppressWarnings("unchecked")  // The type works when deserializing, so it works when serializing
  public static <B, T> StringLoadable<T> instance(StringLoadable<B> loadable, Class<T> typeClass, String errorMsg) {
    return loadable.comapFlatMap((base, error) -> {
      if (typeClass.isInstance(base)) {
        return typeClass.cast(base);
      }
      throw error.create(errorMsg);
    }, t -> (B)t);
  }
}

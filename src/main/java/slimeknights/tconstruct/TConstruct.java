package slimeknights.tconstruct;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.TConstructDataGen;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerAttachments;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionLoader;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.plugin.craftingtweaks.CraftingTweaksPlugin;
import slimeknights.tconstruct.plugin.jsonthings.JsonThingsPlugin;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * TConstruct, the tool mod. Craft your tools with style, then modify until the original is gone!
 *
 * @author mDiyo
 */

@Mod(TConstruct.MOD_ID)
@net.neoforged.fml.common.EventBusSubscriber()
public class TConstruct {

  public static final String MOD_ID = "tconstruct";
  public static final Logger LOG = LogManager.getLogger(MOD_ID);
  public static final Random RANDOM = new Random();

  /* Instance of this mod, used for grabbing prototype fields */
  public static TConstruct instance;

  public TConstruct(IEventBus bus, ModContainer container) {
    instance = this;

    Config.init(container);
    TinkerItemDisplays.init();
    MaterialRegistry.init();

    // initialize modules, done this way rather than with annotations to give us control over the order
    // base
    bus.register(new TinkerCommons());
    bus.register(new TinkerMaterials());
    new TinkerEffects();
    bus.register(new TinkerGadgets());
    bus.register(new TinkerAttributes());
    // world
    bus.register(new TinkerWorld());
    new TinkerStructures(bus);
    // tools
    bus.register(new TinkerTables());
    bus.register(new TinkerModifiers());
    new TinkerToolParts();
    bus.register(new TinkerTools());
    // smeltery
    bus.register(new TinkerSmeltery());
    bus.register(new TinkerFluids());

    // init deferred registers
    TinkerAttachments.register(bus);
    TinkerModule.initRegisters(bus);
    TinkerNetwork.setup();
    TinkerTags.init();
    TConstructDataGen.init(bus);
    NeoForge.EVENT_BUS.addListener(TConstruct::normalizeLegacyRecipeJsons);
    NeoForge.EVENT_BUS.addListener(TConstruct::syncRecipeContent);
    // init client logic

    // Keep optional integrations behind loader checks so their API classes are
    // never resolved when the corresponding mod is absent.
    if (ModList.get().isLoaded("craftingtweaks")) {
      CraftingTweaksPlugin.onConstruct();
    }
    if (ModList.get().isLoaded("jsonthings")) {
      JsonThingsPlugin.onConstruct();
    }

    // TODO 26.1: re-enable the remaining optional integrations when their
    // upstream dependencies publish compatible 26.1 APIs.
  }

  @SubscribeEvent
  static void commonSetup(final FMLCommonSetupEvent event) {
    ToolDefinitionLoader.init();
    StationSlotLayoutLoader.init();
  }

  /**
   * 26.1 no longer sends the full recipe manager to clients by default.
   * Request the Tinkers recipe types needed by JEI and the in-game book.
   */
  private static void syncRecipeContent(OnDatapackSyncEvent event) {
    event.sendRecipes(TinkerRecipeTypes.getAllTypes());
  }

  /**
   * Converts the legacy 1.20.1 recipe condition formats before NeoForge's
   * conditional recipe codec starts deserializing recipe bodies.
   */
  private static void normalizeLegacyRecipeJsons(ModifyRecipeJsonsEvent event) {
    ICondition.IContext context;
    try {
      context = ConditionalOps.retrieveContext().codec().parse(event.getOps(), event.getOps().emptyMap())
        .getOrThrow(JsonParseException::new);
    } catch (RuntimeException exception) {
      LOG.warn("Unable to obtain the NeoForge condition context while migrating legacy recipes", exception);
      context = ICondition.IContext.EMPTY;
    }

    for (Map.Entry<Identifier, JsonElement> entry : List.copyOf(event.getRecipeJsons().entrySet())) {
      if (!MOD_ID.equals(entry.getKey().getNamespace()) || !entry.getValue().isJsonObject()) {
        continue;
      }
      JsonObject recipe = entry.getValue().getAsJsonObject();
      JsonElement typeElement = recipe.get("type");
      if (typeElement != null && typeElement.isJsonPrimitive() && typeElement.getAsJsonPrimitive().isString()
          && "forge:conditional".equals(typeElement.getAsString())) {
        JsonElement rootConditions = recipe.get("conditions");
        if (rootConditions != null && !matchesLegacyConditions(rootConditions, event.getOps(), context)) {
          event.getRecipeJsons().remove(entry.getKey());
          continue;
        }
        JsonArray branches = recipe.getAsJsonArray("recipes");
        JsonObject selected = null;
        if (branches != null) {
          for (JsonElement branchElement : branches) {
            if (!branchElement.isJsonObject()) {
              continue;
            }
            JsonObject branch = branchElement.getAsJsonObject();
            JsonElement conditions = branch.get("conditions");
            if (conditions == null || matchesLegacyConditions(conditions, event.getOps(), context)) {
              JsonElement nested = branch.get("recipe");
              if (nested != null && nested.isJsonObject()) {
                selected = nested.getAsJsonObject().deepCopy();
                break;
              }
            }
          }
        }
        if (selected == null) {
          event.getRecipeJsons().remove(entry.getKey());
        } else {
          moveLegacyConditions(selected);
          normalizeLegacyNativeCraftingRecipe(selected);
          normalizeLegacyRecipePayload(selected);
          event.getRecipeJsons().put(entry.getKey(), selected);
        }
      } else {
        moveLegacyConditions(recipe);
        normalizeLegacyNativeCraftingRecipe(recipe);
      }
      normalizeLegacyRecipePayload(recipe);
    }
  }

  private static void moveLegacyConditions(JsonObject recipe) {
    if (!recipe.has("neoforge:conditions") && recipe.has("conditions") && recipe.get("conditions").isJsonArray()) {
      recipe.add("neoforge:conditions", recipe.remove("conditions"));
    }
  }

  /** Converts the small set of legacy shapes still consumed by vanilla recipes. */
  private static void normalizeLegacyNativeCraftingRecipe(JsonObject recipe) {
    JsonElement type = recipe.get("type");
    if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
      return;
    }
    String typeName = type.getAsString();
    boolean nativeRecipe = isNativeRecipeType(typeName);
    boolean mantleCraftingRecipe = typeName.startsWith("mantle:crafting_");
    if (!nativeRecipe && !mantleCraftingRecipe) {
      return;
    }
    if ("minecraft:crafting_shaped".equals(typeName) || typeName.startsWith("mantle:crafting_shaped")) {
      JsonElement keyElement = recipe.get("key");
      if (keyElement != null && keyElement.isJsonObject()) {
        for (Map.Entry<String, JsonElement> entry : List.copyOf(keyElement.getAsJsonObject().entrySet())) {
          keyElement.getAsJsonObject().add(entry.getKey(), IngredientLoadable.normalizeLegacyIngredientForRecipe(entry.getValue()));
        }
      }
      // Shaped retextured recipes use the same Ingredient codec for their
      // texture source as for the pattern key. Normalize legacy tag syntax
      // here as well, otherwise only these recipes fail during reload.
      normalizeIngredientField(recipe, "texture");
    } else if ("minecraft:crafting_shapeless".equals(typeName) || typeName.startsWith("mantle:crafting_shapeless")) {
      JsonElement ingredients = recipe.get("ingredients");
      if (ingredients != null && ingredients.isJsonArray()) {
        JsonArray normalized = new JsonArray();
        for (JsonElement ingredient : ingredients.getAsJsonArray()) {
          normalized.add(IngredientLoadable.normalizeLegacyIngredient(ingredient));
        }
        recipe.add("ingredients", normalized);
      }
    } else {
      normalizeIngredientField(recipe, "ingredient");
      normalizeIngredientField(recipe, "template");
      normalizeIngredientField(recipe, "base");
      normalizeIngredientField(recipe, "addition");
      normalizeIngredientField(recipe, "tool");
      normalizeIngredientField(recipe, "pattern");
      normalizeIngredientField(recipe, "material");
      normalizeIngredientField(recipe, "texture");
    }

    normalizeLegacyOutputField(recipe, "result", nativeRecipe);
  }

  /** Applies ID-only compatibility to custom Mantle/Tinkers recipes. */
  private static void normalizeLegacyRecipePayload(JsonObject recipe) {
    normalizeLegacyItemIds(recipe);
  }

  /** Converts legacy ItemStack result objects without touching ingredient objects. */
  private static void normalizeLegacyOutputField(JsonObject recipe, String field, boolean nativeOutput) {
    if (!nativeOutput) {
      return;
    }
    JsonElement output = recipe.get(field);
    if (output == null) {
      return;
    }
    if (output.isJsonObject()) {
      normalizeLegacyOutputObject(output.getAsJsonObject(), nativeOutput);
    } else if (output.isJsonArray()) {
      for (JsonElement value : output.getAsJsonArray()) {
        if (value.isJsonObject()) {
          normalizeLegacyOutputObject(value.getAsJsonObject(), nativeOutput);
        }
      }
    }
  }

  private static void normalizeLegacyOutputObject(JsonObject output, boolean nativeOutput) {
    if (nativeOutput && !output.has("id") && output.has("item") && output.get("item").isJsonPrimitive()) {
      output.add("id", output.remove("item"));
    }
    for (JsonElement child : output.asMap().values()) {
      if (child.isJsonObject()) {
        normalizeLegacyOutputObject(child.getAsJsonObject(), nativeOutput);
      } else if (child.isJsonArray()) {
        for (JsonElement value : child.getAsJsonArray()) {
          if (value.isJsonObject()) {
            normalizeLegacyOutputObject(value.getAsJsonObject(), nativeOutput);
          }
        }
      }
    }
  }

  /** Renames removed vanilla item IDs anywhere in a recipe payload. */
  private static void normalizeLegacyItemIds(JsonElement element) {
    if (element.isJsonObject()) {
      for (Map.Entry<String, JsonElement> entry : List.copyOf(element.getAsJsonObject().entrySet())) {
        JsonElement value = entry.getValue();
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
          String id = value.getAsString();
          if ("minecraft:scute".equals(id)) {
            element.getAsJsonObject().addProperty(entry.getKey(), "minecraft:turtle_scute");
          } else if ("minecraft:chain".equals(id)) {
            element.getAsJsonObject().addProperty(entry.getKey(), "minecraft:iron_chain");
          }
        } else {
          normalizeLegacyItemIds(value);
        }
      }
    } else if (element.isJsonArray()) {
      for (int i = 0; i < element.getAsJsonArray().size(); i++) {
        JsonElement value = element.getAsJsonArray().get(i);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
          String id = value.getAsString();
          if ("minecraft:scute".equals(id)) {
            element.getAsJsonArray().set(i, new com.google.gson.JsonPrimitive("minecraft:turtle_scute"));
          } else if ("minecraft:chain".equals(id)) {
            element.getAsJsonArray().set(i, new com.google.gson.JsonPrimitive("minecraft:iron_chain"));
          }
        } else {
          normalizeLegacyItemIds(value);
        }
      }
    }
  }

  private static boolean isNativeRecipeType(String typeName) {
    return switch (typeName) {
      case "minecraft:crafting_shaped", "minecraft:crafting_shapeless",
        "minecraft:smelting", "minecraft:blasting", "minecraft:smoking",
        "minecraft:campfire_cooking", "minecraft:stonecutting",
        "minecraft:smithing_transform", "minecraft:smithing_trim" -> true;
      default -> false;
    };
  }

  private static void normalizeIngredientField(JsonObject recipe, String field) {
    JsonElement ingredient = recipe.get(field);
    if (ingredient != null && (ingredient.isJsonObject() || ingredient.isJsonArray())) {
      recipe.add(field, IngredientLoadable.normalizeLegacyIngredientForRecipe(ingredient));
    }
  }

  private static boolean matchesLegacyConditions(JsonElement element, RegistryOps<JsonElement> ops, ICondition.IContext context) {
    if (!element.isJsonArray()) {
      throw new JsonParseException("Legacy recipe conditions must be an array");
    }
    List<ICondition> conditions = ICondition.LIST_CODEC.parse(ops, element).getOrThrow(JsonParseException::new);
    return conditions.stream().allMatch(condition -> condition.test(context));
  }

  /* Utils */

  /**
   * Gets a resource location for Tinkers
   * @param name  Resource path
   * @return  Location for tinkers
   */
  @SuppressWarnings("removal")
  public static Identifier getResource(String name) {
    return Identifier.fromNamespaceAndPath(MOD_ID, name);
  }

  /**
   * Gets a data key for the capability, mainly used for modifier markers
   * @param name  Resource path
   * @return  Location for tinkers
   */
  public static <T> TinkerDataKey<T> createKey(String name) {
    return TinkerDataKey.of(getResource(name));
  }

  /**
   * Gets a data key for the capability, mainly used for modifier markers
   * @param name         Resource path
   * @param constructor  Constructor for compute if absent
   * @return  Location for tinkers
   */
  public static <T> ComputableDataKey<T> createKey(String name, Supplier<T> constructor) {
    return ComputableDataKey.of(getResource(name), constructor);
  }

  /**
   * Returns the given Resource prefixed with tinkers resource location. Use this function instead of hardcoding
   * resource locations.
   */
  public static String resourceString(String res) {
    return String.format("%s:%s", MOD_ID, res);
  }

  /**
   * Prefixes the given unlocalized name with tinkers prefix. Use this when passing unlocalized names for a uniform
   * namespace.
   */
  public static String prefix(String name) {
    return MOD_ID + "." + name.toLowerCase(Locale.US);
  }

  /** Makes a Tinker's description ID */
  public static String makeDescriptionId(String type, String name) {
    return type + "." + MOD_ID + "." + name;
  }

  /**
   * Makes a translation key for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static String makeTranslationKey(String base, String name) {
    return Util.makeTranslationKey(base, getResource(name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static MutableComponent makeTranslation(String base, String name) {
    return Component.translatable(makeTranslationKey(base, name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base       Base name, such as "block" or "gui"
   * @param name       Object name
   * @param arguments  Additional arguments to the translation
   * @return  Translation key
   */
  public static MutableComponent makeTranslation(String base, String name, Object... arguments) {
    return Component.translatable(makeTranslationKey(base, name), arguments);
  }

  /**
   * This function is called in the constructor in some internal classes that are a common target for addons to wrongly extend.
   * These classes will cause issues if blindly used by the addon, and are typically trivial for the addon to implement
   * the parts they need if they just put in some effort understanding the code they are copying.
   *
   * As a reminder for addon devs, anything that is not in the library package can and will change arbitrarily. If you need to use a feature outside library, request it on our github.
   * @param self  Class to validate
   */
  public static void sealTinkersClass(Object self, String base, String solution) {
    // note for future maintainers: this does not use Java 9's sealed classes as unless you use modules those are restricted to the same package.
    // Dumb restriction but not like we can change it.
    String name = self.getClass().getName();
    if (!name.startsWith("slimeknights.tconstruct.")) {
      throw new IllegalStateException(base + " being extended from invalid package " + name + ". " + solution);
    }
  }
}

package slimeknights.tconstruct.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.listener.MergingJsonDataLoader;
import slimeknights.mantle.util.JsonHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the existing modifier-model map resources for native 26.1 item models.
 *
 * <p>Keeping these maps as shared resources avoids copying the same modifier
 * definitions into every charge, blocking, and broken item-model state. The
 * merge order deliberately matches the 1.20.1 loader: earlier
 * resource IDs take priority, and an explicit empty model removes a fallback.</p>
 */
public final class NativeModifierModelMapManager extends MergingJsonDataLoader<NativeModifierModelMapManager.Builder> {
  public static final NativeModifierModelMapManager INSTANCE = new NativeModifierModelMapManager();

  private Map<Identifier, ModelMap> models = Map.of();
  private final Map<List<Identifier>, ModelMap> resolved = new ConcurrentHashMap<>();

  private NativeModifierModelMapManager() {
    super(JsonHelper.DEFAULT_GSON, "tinkering/modifier_models", id -> new Builder());
  }

  static final class Builder {
    private final Map<String, JsonElement> constants = new LinkedHashMap<>();
    private final Map<Identifier, JsonElement> modifiers = new LinkedHashMap<>();
  }

  /** Resolved native definitions, retaining constant keys solely for merge order. */
  public record ModelMap(Map<String, NativeModifierModel> constants,
                         Map<Identifier, NativeModifierModel> modifiers,
                         List<NativeModifierModel> constantModels) {
    private static final ModelMap EMPTY = new ModelMap(Map.of(), Map.of(), List.of());

    private static ModelMap create(Map<String, NativeModifierModel> constants,
                                   Map<Identifier, NativeModifierModel> modifiers) {
      Map<String, NativeModifierModel> orderedConstants = constants.isEmpty() ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(constants));
      List<NativeModifierModel> sortedConstants = orderedConstants.entrySet().stream()
        .sorted(Entry.<String,NativeModifierModel>comparingByKey().reversed())
        .map(Entry::getValue)
        .toList();
      return new ModelMap(orderedConstants, modifiers.isEmpty() ? Map.of() : Map.copyOf(modifiers), sortedConstants);
    }
  }

  private static <T> void insert(Map<T, JsonElement> map, T key, JsonElement value) {
    if (value.isJsonNull()) {
      map.remove(key);
    } else {
      map.put(key, value);
    }
  }

  @Override
  protected void parse(Builder builder, Identifier id, JsonElement element) throws JsonSyntaxException {
    JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
    if (json.has("constant")) {
      for (Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "constant").entrySet()) {
        insert(builder.constants, entry.getKey(), entry.getValue());
      }
    }
    if (json.has("modifiers")) {
      for (Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "modifiers").entrySet()) {
        Identifier modifier = Identifier.tryParse(entry.getKey());
        if (modifier == null) {
          throw new JsonSyntaxException("Invalid modifier ID '" + entry.getKey() + "' in " + id);
        }
        insert(builder.modifiers, modifier, entry.getValue());
      }
    }
  }

  @Override
  protected void finishLoad(Map<Identifier, Builder> builders, ResourceManager manager) {
    Map<Identifier, ModelMap> parsed = new HashMap<>();
    for (Entry<Identifier, Builder> file : builders.entrySet()) {
      Map<String, NativeModifierModel> constants = new LinkedHashMap<>();
      Map<Identifier, NativeModifierModel> modifiers = new LinkedHashMap<>();
      file.getValue().constants.forEach((key, value) -> constants.put(key, NativeModifierModel.fromJson(value)));
      file.getValue().modifiers.forEach((key, value) -> modifiers.put(key, NativeModifierModel.fromJson(value)));
      if (!constants.isEmpty() || !modifiers.isEmpty()) {
        parsed.put(file.getKey(), ModelMap.create(constants, modifiers));
      }
    }
    this.models = Map.copyOf(parsed);
    this.resolved.clear();
  }

  /** Resolves a prioritized list of shared maps using the legacy merge rules. */
  public ModelMap get(List<Identifier> options) {
    if (options.isEmpty()) {
      return ModelMap.EMPTY;
    }
    return resolved.computeIfAbsent(List.copyOf(options), this::resolve);
  }

  private ModelMap resolve(List<Identifier> options) {
    Map<String, NativeModifierModel> constants = new LinkedHashMap<>();
    Map<Identifier, NativeModifierModel> modifiers = new LinkedHashMap<>();
    for (int index = options.size() - 1; index >= 0; index--) {
      ModelMap map = models.get(options.get(index));
      if (map != null) {
        constants.putAll(map.constants());
        modifiers.putAll(map.modifiers());
      }
    }
    constants.entrySet().removeIf(entry -> entry.getValue().definition() instanceof NativeModifierModel.Empty);
    modifiers.entrySet().removeIf(entry -> entry.getValue().definition() instanceof NativeModifierModel.Empty);
    if (constants.isEmpty() && modifiers.isEmpty()) {
      return ModelMap.EMPTY;
    }
    return ModelMap.create(constants, modifiers);
  }
}

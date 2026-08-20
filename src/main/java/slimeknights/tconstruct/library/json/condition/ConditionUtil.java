package slimeknights.tconstruct.library.json.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.Arrays;
import java.util.List;

/** Native codec helpers for Tinkers data files that store standalone conditions. */
public final class ConditionUtil {
  private ConditionUtil() {}

  public static JsonElement serialize(ICondition condition) {
    return ICondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow(JsonParseException::new);
  }

  public static JsonElement serialize(ICondition... conditions) {
    return serialize(Arrays.asList(conditions));
  }

  public static JsonElement serialize(List<ICondition> conditions) {
    return ICondition.LIST_CODEC.encodeStart(JsonOps.INSTANCE, conditions).getOrThrow(JsonParseException::new);
  }

  public static ICondition deserialize(JsonElement json) {
    return ICondition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
  }

  public static boolean testConditions(JsonObject json, String key, ICondition.IContext context) {
    if (!json.has(key)) {
      return true;
    }
    List<ICondition> conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.get(key)).getOrThrow(JsonParseException::new);
    return conditions.stream().allMatch(condition -> condition.test(context));
  }
}

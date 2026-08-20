package slimeknights.tconstruct.library.json;

import com.google.gson.JsonObject;
import lombok.Data;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;

/** Represents a redirect in a material or modifier JSON */
@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
public class JsonRedirect {
  private final Identifier id;
  @Nullable
  private final ICondition condition;

  public JsonRedirect(Identifier id, @Nullable ICondition condition) {
    this.id = id;
    this.condition = condition;
  }

  public Identifier getId() { return id; }
  @Nullable
  public ICondition getCondition() { return condition; }

  /** Serializes this to JSON */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("id", id.toString());
    if (condition != null) {
      json.add("condition", slimeknights.tconstruct.library.json.condition.ConditionUtil.serialize(condition));
    }
    return json;
  }

  /** Deserializes this to JSON */
  public static JsonRedirect fromJson(JsonObject json) {
    Identifier id = JsonHelper.getIdentifier(json, "id");
    ICondition condition = null;
    if (json.has("condition")) {
      condition = slimeknights.tconstruct.library.json.condition.ConditionUtil.deserialize(json);
    }
    return new JsonRedirect(id, condition);
  }
}

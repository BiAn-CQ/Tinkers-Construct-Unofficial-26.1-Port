package slimeknights.tconstruct.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.ArrayList;
import java.util.List;

/** Encodes an advancement with NeoForge datapack conditions. */
public class ConditionalAdvancement {
  public static class Builder {
    private final List<ICondition> conditions = new ArrayList<>();
    private Advancement.Builder advancement;

    public Builder addCondition(ICondition condition) {
      this.conditions.add(condition);
      return this;
    }

    public Builder addAdvancement(Advancement.Builder advancement) {
      this.advancement = advancement;
      return this;
    }

    public JsonObject write(Identifier id, HolderLookup.Provider registries) {
      var ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registries);
      JsonObject json = advancement == null ? new JsonObject() : Advancement.CODEC.encodeStart(ops, advancement.build(id).value()).getOrThrow().getAsJsonObject();
      if (!conditions.isEmpty()) {
        JsonArray array = new JsonArray();
        for (ICondition condition : conditions) {
          array.add(ICondition.CODEC.encodeStart(ops, condition).getOrThrow());
        }
        json.add("conditions", array);
      }
      return json;
    }
  }
}

package slimeknights.tconstruct.world.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.logic.AncientToolTradeWeightCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Generates the weighted, data-driven 26.1 wandering trader ancient-tool trades. */
public class AncientToolTradeProvider extends GenericDataProvider {
  private static final String TRADE_PREFIX = "wandering_trader/ancient_tool_";
  private final PackOutput.PathProvider tagPathProvider;

  public AncientToolTradeProvider(PackOutput output) {
    super(output, Target.DATA_PACK, "villager_trade");
    this.tagPathProvider = output.createPathProvider(Target.DATA_PACK, "tags/villager_trade");
  }

  private static Identifier tradeId(int index) {
    return TConstruct.getResource(TRADE_PREFIX + String.format(Locale.ROOT, "%03d", index));
  }

  private static JsonObject trade(int minimumWeight) {
    JsonObject wants = new JsonObject();
    wants.addProperty("id", "minecraft:emerald");

    JsonObject gives = new JsonObject();
    // The loot function replaces this placeholder. Keeping a valid modifiable
    // item makes the trade remain structurally valid if another pack inspects it.
    gives.addProperty("id", "tconstruct:pickaxe");

    JsonObject predicate = new JsonObject();
    predicate.addProperty("condition", "tconstruct:ancient_tool_trade_weight");
    predicate.addProperty("minimum", minimumWeight);

    JsonObject function = new JsonObject();
    function.addProperty("function", "tconstruct:ancient_tool_trade");
    JsonArray functions = new JsonArray();
    functions.add(function);

    JsonObject trade = new JsonObject();
    trade.add("wants", wants);
    trade.add("gives", gives);
    trade.addProperty("max_uses", 1);
    trade.addProperty("xp", 15);
    trade.addProperty("reputation_discount", 1.0f);
    trade.add("merchant_predicate", predicate);
    trade.add("given_item_modifiers", functions);
    return trade;
  }

  private static JsonObject uncommonTag() {
    JsonArray values = new JsonArray();
    for (int index = 1; index <= AncientToolTradeWeightCondition.MAX_WEIGHT; index++) {
      values.add(tradeId(index).toString());
    }
    JsonObject tag = new JsonObject();
    tag.addProperty("replace", false);
    tag.add("values", values);
    return tag;
  }

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    List<CompletableFuture<?>> tasks = new ArrayList<>(AncientToolTradeWeightCondition.MAX_WEIGHT + 1);
    for (int index = 1; index <= AncientToolTradeWeightCondition.MAX_WEIGHT; index++) {
      tasks.add(saveJson(output, tradeId(index), trade(index)));
    }
    tasks.add(DataProvider.saveStable(output, uncommonTag(),
      tagPathProvider.json(Identifier.withDefaultNamespace("wandering_trader/uncommon"))));
    return allOf(tasks);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct ancient tool trades";
  }
}

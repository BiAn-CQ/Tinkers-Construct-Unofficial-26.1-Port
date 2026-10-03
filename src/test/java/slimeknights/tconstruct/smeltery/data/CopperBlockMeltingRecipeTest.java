package slimeknights.tconstruct.smeltery.data;

import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class CopperBlockMeltingRecipeTest extends CoreTestBootstrap {
  @Test
  void everyWeatheringAndWaxingVariantIsRegisteredAndHasConservativeYield() throws Exception {
    Map<String,Integer> amounts = Map.ofEntries(
      Map.entry("chiseled", 200), Map.entry("grate", 200), Map.entry("bulb", 600),
      Map.entry("door", 180), Map.entry("trapdoor", 360), Map.entry("chest", 720),
      Map.entry("bars", 30), Map.entry("chain", 110), Map.entry("lantern", 80),
      Map.entry("lightning_rod", 270), Map.entry("golem_statue", 90), Map.entry("torch", 2));
    for (var entry : amounts.entrySet()) {
      try (var stream = getClass().getClassLoader().getResourceAsStream(
        "data/tconstruct/recipe/smeltery/melting/metal/copper/" + entry.getKey() + ".json")) {
        assertThat(stream).isNotNull();
        var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        assertThat(json.getAsJsonObject("result").get("amount").getAsInt()).isEqualTo(entry.getValue());
        var ingredient = json.get("ingredient");
        if (ingredient.isJsonObject()) {
          var variants = ingredient.getAsJsonObject().getAsJsonArray("ingredients");
          assertThat(variants.size()).isEqualTo(8);
          for (var id : variants) {
            assertThat(BuiltInRegistries.ITEM.containsKey(Identifier.parse(id.getAsString()))).as(id.getAsString()).isTrue();
          }
        }
      }
    }
    assertThat(amounts.get("chest") + amounts.get("golem_statue")).isLessThanOrEqualTo(810);
    assertThat(amounts.get("grate") * 4).isLessThanOrEqualTo(810);
    assertThat(amounts.get("torch") * 4).isLessThanOrEqualTo(10);
  }
}

package slimeknights.tconstruct.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class UpstreamCosmeticResourcesTest {
  @Test
  void oldFeatherFallingKeepsItsTwoLevelsOfNewTraitPerLevel() throws Exception {
    var legacy = read("data/tconstruct/tinkering/modifiers/feather_falling.json");
    var trait = legacy.getAsJsonArray("modules").get(0).getAsJsonObject();
    assertThat(trait.get("name").getAsString()).isEqualTo("tconstruct:feather_fall");
    assertThat(trait.get("level").getAsInt()).isEqualTo(2);
    var recipe = read("data/tconstruct/recipe/tools/modifiers/upgrade/feather_fall.json");
    assertThat(recipe.get("needed_per_level").getAsInt()).isEqualTo(12);
    assertThat(recipe.getAsJsonArray("levels").size()).isEqualTo(4);
  }
  @Test
  void ichorRestoresSixtyOverslimeAndFriendIsVisibleInAdvancedViews() throws Exception {
    assertThat(read("data/tconstruct/recipe/tools/modifiers/slotless/overslime/ichor_ball.json")
      .get("restore_amount").getAsInt()).isEqualTo(60);
    assertThat(read("data/tconstruct/tinkering/modifiers/overslime_friend.json")
      .get("show_in_tooltips").getAsString()).isEqualTo("advanced");
  }
  private JsonObject read(String path) throws Exception {
    try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as(path).isNotNull();
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  @Test
  void travelersDyeSelectsWoolLayerForIntactAndBrokenGear() throws Exception {
    for (String part : new String[]{"goggles", "vest", "pants", "boots", "shield"}) {
      for (String suffix : new String[]{"", "_broken"}) {
        var model = read("assets/tconstruct/tinkering/modifier_models/travelers/" + part + suffix + ".json")
          .getAsJsonObject("constant").getAsJsonObject("__dyed").getAsJsonObject("model");
        assertThat(model.get("type").getAsString()).isEqualTo("tconstruct:material_has_fallback");
        assertThat(model.get("fallback").getAsString()).isEqualTo("wool");
        assertThat(model.get("index").getAsInt()).isEqualTo(1);
      }
    }
  }

  @Test
  void cosmeticModifiersOnlyDisplayInStation() throws Exception {
    for (String modifier : new String[]{"dyed", "embellishment", "banner"}) {
      var contexts = read("data/tconstruct/tinkering/modifiers/" + modifier + ".json").getAsJsonArray("show_in_tooltips");
      assertThat(contexts.size()).isEqualTo(1);
      assertThat(contexts.get(0).getAsString()).isEqualTo("tinker_station");
    }
  }
}

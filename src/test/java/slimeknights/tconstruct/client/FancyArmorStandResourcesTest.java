package slimeknights.tconstruct.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FancyArmorStandResourcesTest {
  @Test
  void allItemDefinitionsResolveToTheStandTextures() throws IOException {
    for (String variant : new String[] {"clear", "bamboo", "bone", "necrotic_bone"}) {
      assertItemDefinition(variant);
    }
  }

  private void assertItemDefinition(String variant) throws IOException {
    String item = variant + "_armor_stand";
    JsonObject reference = loadJson("assets/tconstruct/items/" + item + ".json").getAsJsonObject("model");
    assertThat(reference.get("type").getAsString()).isEqualTo("minecraft:model");
    assertThat(reference.get("model").getAsString()).isEqualTo("tconstruct:item/" + item);

    JsonObject model = loadJson("assets/tconstruct/models/item/" + item + ".json");
    assertThat(model.get("parent").getAsString()).isEqualTo("minecraft:item/generated");
    assertThat(model.getAsJsonObject("textures").get("layer0").getAsString())
      .isEqualTo("tconstruct:item/gadgets/" + item);
    assertThat(getClass().getClassLoader().getResource("assets/tconstruct/textures/item/gadgets/" + item + ".png"))
      .isNotNull();
  }

  private static JsonObject loadJson(String path) throws IOException {
    try (InputStream stream = FancyArmorStandResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IOException("Missing resource " + path);
      }
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }
}

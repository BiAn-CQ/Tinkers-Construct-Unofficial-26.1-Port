package slimeknights.tconstruct.smeltery.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LodestoneMeltingRecipeTest {
  private static final String IRON_RECIPE = "data/tconstruct/recipe/smeltery/melting/metal/iron/lodestone.json";
  private static final String OBSOLETE_NETHERITE_RECIPE = "data/tconstruct/recipe/smeltery/melting/metal/netherite/lodestone.json";

  @Test
  void lodestoneMeltsIntoOneIronIngot() throws IOException {
    ClassLoader loader = getClass().getClassLoader();
    try (InputStream stream = loader.getResourceAsStream(IRON_RECIPE)) {
      assertThat(stream).as("generated lodestone melting recipe").isNotNull();
      String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(json).contains("\"ingredient\": \"minecraft:lodestone\"")
                      .contains("\"amount\": 90")
                      .contains("\"tag\": \"c:molten_iron\"")
                      .doesNotContain("molten_netherite");
    }
    assertThat(loader.getResource(OBSOLETE_NETHERITE_RECIPE)).isNull();
  }
}

package slimeknights.tconstruct.plugin.ftbultimine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FTBUltimineResourcesTest {
  @Test
  void includesTinkerHarvestToolsInUltimine() throws IOException {
    JsonObject tag = loadJson("data/ftbultimine/tags/item/included_tools.json");

    assertThat(tag.get("replace").getAsBoolean()).isFalse();
    assertThat(tag.getAsJsonArray("values"))
      .extracting(element -> element.getAsString())
      .containsExactly("#tconstruct:modifiable/harvest");
  }

  private static JsonObject loadJson(String path) throws IOException {
    try (InputStream stream = resource(path)) {
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static InputStream resource(String path) throws IOException {
    InputStream stream = FTBUltimineResourcesTest.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IOException("Missing resource " + path);
    }
    return stream;
  }
}

package slimeknights.tconstruct.plugin.ftbultimine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

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

  @Test
  void declaresUltimineAsOptionalDependency() throws IOException {
    String metadata = loadTConstructMetadata();
    int dependency = metadata.indexOf("modId=\"ftbultimine\"");

    assertThat(dependency).isNotNegative();
    assertThat(metadata.substring(dependency, Math.min(metadata.length(), dependency + 180)))
      .contains("type=\"optional\"")
      .contains("side=\"BOTH\"");
  }

  private static JsonObject loadJson(String path) throws IOException {
    try (InputStream stream = resource(path)) {
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static String loadTConstructMetadata() throws IOException {
    Enumeration<URL> resources = FTBUltimineResourcesTest.class.getClassLoader()
      .getResources("META-INF/neoforge.mods.toml");
    while (resources.hasMoreElements()) {
      try (InputStream stream = resources.nextElement().openStream()) {
        String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        if (metadata.contains("modId=\"tconstruct\"")) {
          return metadata;
        }
      }
    }
    throw new IOException("Missing Tinkers' Construct mod metadata");
  }

  private static InputStream resource(String path) throws IOException {
    InputStream stream = FTBUltimineResourcesTest.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IOException("Missing resource " + path);
    }
    return stream;
  }
}

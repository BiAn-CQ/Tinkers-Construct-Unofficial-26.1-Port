package slimeknights.tconstruct.tools.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ChineseLanguageTest {
  private static final Pattern FORMAT_ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?(?:\\.\\d+)?([A-Za-z])");

  @Test
  void simplifiedChineseIncludesEveryEnglishKey() throws IOException {
    JsonObject english = load("en_us");
    JsonObject chinese = load("zh_cn");

    assertThat(chinese.keySet()).containsAll(english.keySet());
  }

  @Test
  void simplifiedChinesePreservesFormatArguments() throws IOException {
    JsonObject english = load("en_us");
    JsonObject chinese = load("zh_cn");

    List<String> mismatches = new ArrayList<>();
    for (String key : english.keySet()) {
      if (chinese.has(key)) {
        List<String> englishArguments = formatArguments(english.get(key).getAsString());
        List<String> chineseArguments = formatArguments(chinese.get(key).getAsString());
        if (!englishArguments.equals(chineseArguments)) {
          mismatches.add(key + ": " + englishArguments + " != " + chineseArguments);
        }
      }
    }

    assertThat(mismatches).isEmpty();
  }

  private static JsonObject load(String locale) throws IOException {
    String path = "assets/tconstruct/lang/" + locale + ".json";
    try (InputStream stream = ChineseLanguageTest.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IOException("Missing language resource " + path);
      }
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static List<String> formatArguments(String text) {
    List<String> arguments = new ArrayList<>();
    Matcher matcher = FORMAT_ARGUMENT.matcher(text);
    int implicitIndex = 1;
    while (matcher.find()) {
      String explicitIndex = matcher.group(1);
      int index = explicitIndex == null ? implicitIndex++ : Integer.parseInt(explicitIndex);
      arguments.add(index + ":" + matcher.group(2).toLowerCase());
    }
    Collections.sort(arguments);
    return arguments;
  }
}

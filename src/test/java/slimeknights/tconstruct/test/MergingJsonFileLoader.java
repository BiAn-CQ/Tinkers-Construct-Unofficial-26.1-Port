package slimeknights.tconstruct.test;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.tconstruct.library.utils.ResourceId;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.mockito.Mockito.mock;

/**
 * Loads merging JSON data from test resources, including an optional second data-pack layer.
 *
 * @param <B> builder type used by the production data loader
 */
public class MergingJsonFileLoader<B> extends JsonFileLoader {
  private final MergingJsonTestAdapter<B> dataLoader;

  public MergingJsonFileLoader(MergingJsonTestAdapter<B> dataLoader) {
    super(dataLoader.gsonForTest(), dataLoader.folderForTest());
    this.dataLoader = dataLoader;
  }

  /** Loads files addressed by Tinkers' type-safe identifier wrappers. */
  public void loadAndParseFiles(@Nullable String mergeFolder, ResourceId... files) {
    loadAndParseFiles(mergeFolder, Arrays.stream(files).map(ResourceId::location).toArray(Identifier[]::new));
  }

  /** Loads and parses the relevant files into the data loader. */
  public void loadAndParseFiles(@Nullable String mergeFolder, Identifier... files) {
    Map<Identifier,B> parsedMap = new HashMap<>();
    for (Entry<Identifier,JsonElement> entry : loadFilesAsSplashlist(files).entrySet()) {
      Identifier id = entry.getKey();
      B builder = parsedMap.computeIfAbsent(id, dataLoader.builderConstructorForTest());
      dataLoader.parseForTest(builder, id, entry.getValue());
    }
    if (mergeFolder != null) {
      JsonFileLoader secondDataPack = new JsonFileLoader(
        dataLoader.gsonForTest(), dataLoader.folderForTest() + "/" + mergeFolder);
      for (Entry<Identifier,JsonElement> entry : secondDataPack.loadFilesAsSplashlist(files).entrySet()) {
        Identifier id = entry.getKey();
        B builder = parsedMap.computeIfAbsent(id, dataLoader.builderConstructorForTest());
        dataLoader.parseForTest(builder, id, entry.getValue());
      }
    }
    dataLoader.finishLoadForTest(parsedMap, mock(ResourceManager.class));
  }
}

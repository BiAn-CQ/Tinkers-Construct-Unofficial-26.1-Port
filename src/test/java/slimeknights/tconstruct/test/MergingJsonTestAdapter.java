package slimeknights.tconstruct.test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.function.Function;

/**
 * Test-only view of Mantle's protected merging-loader hooks.
 *
 * <p>Concrete TConstruct managers expose these hooks through a small subclass in their own
 * test package. This keeps the fixture out of Mantle's module packages and avoids a Java
 * split-package conflict.</p>
 */
public interface MergingJsonTestAdapter<B> {
  Gson gsonForTest();

  String folderForTest();

  Function<Identifier,B> builderConstructorForTest();

  void parseForTest(B builder, Identifier id, JsonElement element);

  void finishLoadForTest(Map<Identifier,B> map, ResourceManager manager);
}

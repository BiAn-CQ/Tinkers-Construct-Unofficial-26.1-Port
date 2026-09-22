package slimeknights.tconstruct.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.io.BufferedReader;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AddonArmorLayerParsingTest {
  @Test
  void precoloredArmorRemainsVisibleWithoutVanillaDye() throws Exception {
    Minecraft minecraft = mock(Minecraft.class);
    ResourceManager resources = mock(ResourceManager.class);
    Resource model = mock(Resource.class);
    when(minecraft.getResourceManager()).thenReturn(resources);
    when(resources.getResource(Identifier.parse("example:tinkering/armor_models/armor.json")))
      .thenReturn(Optional.of(model));
    when(model.openAsReader()).thenReturn(new BufferedReader(new StringReader("""
      {"layers":[{"type":"tconstruct:fixed","prefix":"example:plate_"}]}
      """)));
    when(resources.getResource(Identifier.parse("example:textures/tinker_armor/plate_armor.png")))
      .thenReturn(Optional.of(mock(Resource.class)));
    try (var client = mockStatic(Minecraft.class)) {
      client.when(Minecraft::getInstance).thenReturn(minecraft);
      var extension = new AddonMultilayerArmorClientExtension(Identifier.parse("example:armor"));
      var layer = new EquipmentClientInfo.Layer(Identifier.parse("example:plate_armor"));
      assertThat(extension.getArmorLayerTintColor(mock(ItemStack.class), layer, 0, 0)).isEqualTo(0xFFFFFFFF);
      var unrelated = new EquipmentClientInfo.Layer(Identifier.parse("example:other_armor"));
      assertThat(extension.getArmorLayerTintColor(mock(ItemStack.class), unrelated, 0, 0)).isEqualTo(0xFFFFFFFF);
    }
  }

  @Test
  void firstPresentWithoutPrefixMatchesItsEquipmentLayers() throws Exception {
    Object layer = parse("""
      {"type":"tconstruct:first_present","options":[
        {"type":"tconstruct:dyed","prefix":"example:armor/wood_"},
        {"type":"tconstruct:material","index":2,"prefix":"example:armor/wood_"}
      ]}
      """);
    assertThat(layer).isNotNull();
    for (String suffix : new String[]{"armor", "leggings", "wings"}) {
      assertThat(matches(layer, "example:armor/wood_" + suffix)).isTrue();
    }
    assertThat(matches(layer, "other:armor/wood_armor")).isFalse();
    assertThat(matches(layer, "example:armor/metal_armor")).isFalse();
  }

  @Test
  void unknownPrefixlessOptionDoesNotDiscardMaterialFallback() throws Exception {
    Object layer = parse("""
      {"type":"tconstruct:first_present","options":[
        {"type":"example:unsupported"},
        {"type":"tconstruct:material","index":0,"prefix":"example:plate_"}
      ]}
      """);
    assertThat(matches(layer, "example:plate_armor")).isTrue();
    assertThat(parse("{\"type\":\"example:unsupported\"}")).isNull();
  }

  private static Object parse(String json) throws Exception {
    var method = AddonMultilayerArmorClientExtension.class.getDeclaredMethod("parseLayer", JsonObject.class);
    method.setAccessible(true);
    return method.invoke(null, JsonParser.parseString(json).getAsJsonObject());
  }

  @Test
  void firstPresentUsesAvailableAlternativePrefixAndSuffix() throws Exception {
    Minecraft minecraft = mock(Minecraft.class);
    ResourceManager resources = mock(ResourceManager.class);
    when(minecraft.getResourceManager()).thenReturn(resources);
    when(resources.getResource(Identifier.parse("example:textures/tinker_armor/second_leggings_glow.png")))
      .thenReturn(Optional.of(mock(Resource.class)));
    try (var client = mockStatic(Minecraft.class)) {
      client.when(Minecraft::getInstance).thenReturn(minecraft);
      Object layer = parse("""
        {"type":"tconstruct:first_present","options":[
          {"type":"tconstruct:fixed","prefix":"example:missing_"},
          {"type":"tconstruct:fixed","prefix":"example:second_","suffix":"_glow","luminosity":12}
        ]}
        """);
      Object result = select(layer, mock(ItemStack.class), "example:missing_leggings");
      assertThat(field(result, "texture")).isEqualTo(Identifier.parse("example:second_leggings_glow"));
      assertThat(field(result, "luminosity")).isEqualTo(12);
    }
  }

  @Test
  void dyedLayerUsesCustomKeyDefaultColorAndLuminosity() throws Exception {
    Minecraft minecraft = mock(Minecraft.class);
    ResourceManager resources = mock(ResourceManager.class);
    ItemStack stack = mock(ItemStack.class);
    var tool = mock(slimeknights.tconstruct.library.tools.nbt.ToolStack.class);
    var data = new slimeknights.tconstruct.library.tools.nbt.ToolDataNBT();
    when(tool.getPersistentData()).thenReturn(data);
    when(minecraft.getResourceManager()).thenReturn(resources);
    when(resources.getResource(Identifier.parse("example:textures/tinker_armor/dye_armor.png")))
      .thenReturn(Optional.of(mock(Resource.class)));
    try (var client = mockStatic(Minecraft.class);
         var tools = mockStatic(slimeknights.tconstruct.library.tools.nbt.ToolStack.class)) {
      client.when(Minecraft::getInstance).thenReturn(minecraft);
      tools.when(() -> slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack)).thenReturn(tool);
      Object layer = parse("""
        {"type":"tconstruct:dyed","prefix":"example:dye_","modifier":"example:tint",
         "default_color":"123456","luminosity":15}
        """);
      Object result = select(layer, stack, "example:dye_armor");
      assertThat(field(result, "color")).isEqualTo(0xFF123456);
      assertThat(field(result, "luminosity")).isEqualTo(15);
      data.putInt(Identifier.parse("example:tint"), 0xABCDEF);
      assertThat(field(select(layer, stack, "example:dye_armor"), "color")).isEqualTo(0xFFABCDEF);
    }
  }

  @Test
  void materialFallbackPreservesItsAppliedLayer() throws Exception {
    Object layer = parse("""
      {"type":"tconstruct:material_has_fallback","index":2,"fallback":["metal","rock"],
       "apply":{"type":"tconstruct:fixed","prefix":"example:overlay_","suffix":"_outer"}}
      """);
    assertThat(matches(layer, "example:overlay_armor_outer")).isTrue();
    assertThat(field(layer, "fallbacks")).isEqualTo(java.util.Set.of("metal", "rock"));
    assertThat(field(layer, "index")).isEqualTo(2);
  }

  private static Object select(Object layer, ItemStack stack, String texture) throws Exception {
    var method = layer.getClass().getDeclaredMethod("select", ItemStack.class, Identifier.class);
    method.setAccessible(true);
    return method.invoke(layer, stack, Identifier.parse(texture));
  }

  private static Object field(Object object, String name) throws Exception {
    var method = object.getClass().getDeclaredMethod(name);
    method.setAccessible(true);
    return method.invoke(object);
  }

  private static boolean matches(Object layer, String texture) throws Exception {
    var method = layer.getClass().getDeclaredMethod("matches", Identifier.class);
    method.setAccessible(true);
    return (boolean) method.invoke(layer, Identifier.parse(texture));
  }
}

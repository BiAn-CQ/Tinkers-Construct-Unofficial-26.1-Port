package slimeknights.tconstruct.plugin.jei.material;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import io.netty.buffer.Unpooled;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShapedMaterialsSynchronizationTest extends CoreTestBootstrap {
  @Test
  void travelersRecipesKeepMaterialSlotsAfterNetworkSynchronization() throws Exception {
    var registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    for (String name : List.of("goggles", "chestplate", "pants", "boots", "shield")) {
      var resource = getClass().getResourceAsStream("/data/tconstruct/recipe/tools/armor/travelers/" + name + ".json");
      assertThat(resource).as(name + " recipe resource").isNotNull();
      com.google.gson.JsonElement json;
      try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
        json = JsonParser.parseReader(reader);
      }
      ShapedMaterialsRecipe recipe = ShapedMaterialsRecipe.SERIALIZER.codec().codec().parse(ops, json).getOrThrow();
      var buffer = RegistryFriendlyByteBuf.decorator(registryAccess, ConnectionType.NEOFORGE).apply(Unpooled.buffer());
      try {
        ShapedMaterialsRecipe.SERIALIZER.streamCodec().encode(buffer, recipe);
        ShapedMaterialsRecipe synced = ShapedMaterialsRecipe.SERIALIZER.streamCodec().decode(buffer);
        assertThat(buffer.readableBytes()).as(name + " packet fully consumed").isZero();
        assertThat(synced.getPartCount()).as(name).isEqualTo(recipe.getPartCount());
        for (int part = 0; part < recipe.getPartCount(); part++) {
          int[] expected = ShapedMaterialsExtension.INSTANCE.getMaterialSlots(recipe, recipe.getParts().get(part));
          assertThat(expected).as(name + " source material slots").isNotEmpty();
          assertThat(ShapedMaterialsExtension.INSTANCE.getMaterialSlots(synced, synced.getParts().get(part)))
            .as(name + " synchronized material slots " + part).containsExactly(expected);
        }
      } finally {
        buffer.release();
      }
    }
  }
}

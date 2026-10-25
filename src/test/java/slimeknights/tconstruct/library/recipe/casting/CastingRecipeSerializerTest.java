package slimeknights.tconstruct.library.recipe.casting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.recipe.casting.container.ContainerFillingRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class CastingRecipeSerializerTest extends CoreTestBootstrap {
  private static final String FLUID = "\"fluid\":{\"ingredient\":\"minecraft:water\",\"amount\":90}";

  private record RecipeCase(String name, RecordLoadable<? extends Recipe<?>> loader, Class<?> recipeClass, String json) {}

  @TestFactory
  Stream<DynamicTest> recipeTypesRemainDistinctThroughJsonAndNetwork() {
    var cases = List.of(
      new RecipeCase("item", ItemCastingRecipe.LOADER, ItemCastingRecipe.class,
        "{" + FLUID + ",\"result\":{\"id\":\"minecraft:stone\"},\"cooling_time\":40}"),
      new RecipeCase("retextured", RetexturedCastingRecipe.LOADER, RetexturedCastingRecipe.class,
        "{" + FLUID + ",\"result\":{\"id\":\"minecraft:stone\"},\"cooling_time\":40}"),
      new RecipeCase("duplication", CastDuplicationRecipe.LOADER, CastDuplicationRecipe.class,
        "{" + FLUID + ",\"cast\":\"minecraft:stick\",\"cooling_time\":40}"),
      new RecipeCase("potion", PotionCastingRecipe.LOADER, PotionCastingRecipe.class,
        "{" + FLUID + ",\"bottle\":\"minecraft:glass_bottle\",\"result\":\"minecraft:potion\"}"),
      new RecipeCase("tipping", TippingCastingRecipe.LOADER, TippingCastingRecipe.class,
        "{" + FLUID + ",\"tools\":\"minecraft:stick\",\"modifier\":\"test:tipping\"}"),
      new RecipeCase("clearing", TipClearingCastingRecipe.LOADER, TipClearingCastingRecipe.class,
        "{" + FLUID + ",\"tools\":\"minecraft:stick\",\"modifier\":\"test:tipping\"}"),
      new RecipeCase("container", ContainerFillingRecipe.LOADER, ContainerFillingRecipe.class,
        "{\"container\":\"minecraft:bucket\",\"fluid_amount\":1000}"),
      new RecipeCase("molding", MoldingRecipe.LOADER, MoldingRecipe.class,
        "{\"material\":\"minecraft:stone\",\"result\":{\"id\":\"minecraft:stick\"}}")
    );
    return cases.stream().map(recipeCase -> DynamicTest.dynamicTest(recipeCase.name, () -> {
      RecipeType firstType = new RecipeType<>() {};
      RecipeType secondType = new RecipeType<>() {};
      RecipeSerializer firstSerializer = LoadableRecipeSerializer.of((RecordLoadable)recipeCase.loader, () -> firstType);
      RecipeSerializer secondSerializer = LoadableRecipeSerializer.of((RecordLoadable)recipeCase.loader, () -> secondType);
      roundTrip(recipeCase, firstType, firstSerializer);
      roundTrip(recipeCase, secondType, secondSerializer);
    }));
  }

  private void roundTrip(RecipeCase recipeCase, RecipeType type, RecipeSerializer serializer) {
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    Recipe<?> original = (Recipe<?>)serializer.codec().codec().parse(ops, JsonParser.parseString(recipeCase.json)).getOrThrow();
    assertIdentity(original, type, serializer, recipeCase.recipeClass);

    var encoded = (JsonElement)serializer.codec().codec().encodeStart(ops, original).getOrThrow();
    Recipe<?> reloaded = (Recipe<?>)serializer.codec().codec().parse(ops, encoded).getOrThrow();
    assertIdentity(reloaded, type, serializer, recipeCase.recipeClass);

    var buffer = registryBuffer();
    try {
      serializer.streamCodec().encode(buffer, original);
      Recipe<?> decoded = (Recipe<?>)serializer.streamCodec().decode(buffer);
      assertIdentity(decoded, type, serializer, recipeCase.recipeClass);
      assertThat((JsonElement)serializer.codec().codec().encodeStart(ops, decoded).getOrThrow()).isEqualTo(encoded);
      assertThat(buffer.readableBytes()).isZero();
    } finally {
      buffer.release();
    }
  }

  private static void assertIdentity(Recipe<?> recipe, RecipeType<?> type, RecipeSerializer<?> serializer, Class<?> recipeClass) {
    assertThat(recipe).isExactlyInstanceOf(recipeClass);
    assertThat(recipe.getType()).isSameAs(type);
    assertThat(recipe.getSerializer()).isSameAs(serializer);
  }
}

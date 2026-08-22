package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class MultiAlloyingModuleTest {
  @Test
  void createsMutableCacheFromImmutableRecipeResults() {
    @SuppressWarnings("unchecked")
    RecipeHolder<AlloyRecipe> first = mock(RecipeHolder.class);
    @SuppressWarnings("unchecked")
    RecipeHolder<AlloyRecipe> second = mock(RecipeHolder.class);

    List<RecipeHolder<AlloyRecipe>> cache = MultiAlloyingModule.createRecipeCache(List.of(first, second));

    assertThatCode(() -> Collections.shuffle(cache, new Random(0))).doesNotThrowAnyException();
    Iterator<RecipeHolder<AlloyRecipe>> iterator = cache.iterator();
    iterator.next();
    assertThatCode(iterator::remove).doesNotThrowAnyException();
    assertThat(cache).hasSize(1);
  }
}

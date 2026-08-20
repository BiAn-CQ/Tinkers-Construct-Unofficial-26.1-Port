package slimeknights.tconstruct.library.client.book;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.ingredient.BookStackIngredient;

/** Registers the exact-stack ingredient used only by Tinkers book resources. */
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public final class BookIngredientEvents {
  private BookIngredientEvents() {}

  @SubscribeEvent
  static void registerIngredientType(RegisterEvent event) {
    if (event.getRegistryKey() == NeoForgeRegistries.Keys.INGREDIENT_TYPES) {
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES,
        helper -> helper.register(BookStackIngredient.ID, BookStackIngredient.TYPE));
    }
  }
}

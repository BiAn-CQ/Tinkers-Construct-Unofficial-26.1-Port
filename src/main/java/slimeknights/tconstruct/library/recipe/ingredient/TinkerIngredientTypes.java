package slimeknights.tconstruct.library.recipe.ingredient;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;

/** NeoForge 26.1 custom ingredient type registrations. */
public final class TinkerIngredientTypes {
  private static final DeferredRegister<IngredientType<?>> TYPES =
    DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, TConstruct.MOD_ID);

  public static final DeferredHolder<IngredientType<?>, IngredientType<BlockTagIngredient>> BLOCK_TAG =
    TYPES.register("block_tag", () -> BlockTagIngredient.TYPE);
  public static final DeferredHolder<IngredientType<?>, IngredientType<NoContainerIngredient>> NO_CONTAINER =
    TYPES.register("no_container", () -> NoContainerIngredient.TYPE);

  private TinkerIngredientTypes() {}

  public static void init(IEventBus bus) {
    TYPES.register(bus);
  }
}

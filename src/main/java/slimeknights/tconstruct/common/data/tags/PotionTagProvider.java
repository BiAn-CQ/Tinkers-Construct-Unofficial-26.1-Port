package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.tags.TagEntry;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;

public class PotionTagProvider extends BuiltinRegistryTagProvider<Potion> {
  @SuppressWarnings("deprecation")
  public PotionTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider) {
    super(packOutput, BuiltInRegistries.POTION, lookupProvider, TConstruct.MOD_ID);
  }

  @Override
  protected void addTags(Provider provider) {
    tag(TinkerTags.Potions.HIDDEN_FLUID).add(TagEntry.optionalElement(TinkerTags.HIDDEN_FROM_RECIPE_VIEWERS));
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Potion Tags";
  }
}

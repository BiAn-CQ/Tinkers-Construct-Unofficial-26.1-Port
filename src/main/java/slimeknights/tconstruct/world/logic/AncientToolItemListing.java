package slimeknights.tconstruct.world.logic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Optional;

/**
 * Builds the randomized result for the ancient-tool wandering trader trade.
 * The historical name is retained for compatibility with code that referenced
 * the old {@code VillagerTrades.ItemListing} implementation.
 */
public final class AncientToolItemListing {
  private AncientToolItemListing() {}

  /** Creates a random ancient tool, or an empty stack if the trader tool tag is empty. */
  public static ItemStack createResult(RandomSource random) {
    // step 1: select ancient tool
    Optional<Holder<Item>> selected = BuiltInRegistries.ITEM.get(TinkerTags.Items.TRADER_TOOLS).flatMap(t -> t.getRandomElement(random));
    if (selected.isPresent() && selected.get().value() instanceof IModifiable toolItem) {
      // step 2: select materials
      ToolStack tool = ToolBuildHandler.buildToolRandomMaterials(toolItem, RandomMaterial.ancient(), random);
      // step 3: calculate cost based on tier
      float tier = 0;
      MaterialNBT materials = tool.getMaterials();
      if (materials.isEmpty()) {
        // if no materials, just choose a baseline tier of 2
        tier = 2;
      } else {
        for (MaterialVariant material : materials) {
          tier += material.get().getTier();
        }
        tier /= materials.size();
      }
      // formula is a cost of 6-8 emeralds per tier, meaning cost ranges from 6 (min tier 1) to 32 (max tier 4)
      int cost = Math.round(tier * 6) + random.nextInt(Math.round(2 * tier) + 1);
      ItemStack result = tool.createStack();
      // VillagerTrade adds this value to the base emerald cost, then removes the
      // transient component before exposing the result to the merchant screen.
      result.set(DataComponents.ADDITIONAL_TRADE_COST, cost - 1);
      return result;
    }
    return ItemStack.EMPTY;
  }
}

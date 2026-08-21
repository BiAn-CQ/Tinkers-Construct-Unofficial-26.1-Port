package slimeknights.tconstruct.library.utils;

import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TinkerCapabilityAdaptersTest {
  @Test
  void legacyItemHandlerRoundTripPreservesSpecializedHandler() {
    IChestItemHandler scalingHandler = mock(IChestItemHandler.class);

    IItemHandler converted = TinkerCapabilityAdapters.itemHandler(
      TinkerCapabilityAdapters.itemResource(scalingHandler));

    assertThat(converted).isSameAs(scalingHandler);
  }
}

package slimeknights.tconstruct.tools.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreativeSlotItemTest {
  @Test
  void serverHandlesEveryMenu() {
    assertThat(CreativeSlotItem.shouldHandleStackInteraction(player(false, false, 4))).isTrue();
  }

  @Test
  void creativeClientHandlesUnsyncedInventoryMenu() {
    assertThat(CreativeSlotItem.shouldHandleStackInteraction(player(true, true, 0))).isTrue();
  }

  @Test
  void survivalClientWaitsForServer() {
    assertThat(CreativeSlotItem.shouldHandleStackInteraction(player(true, false, 0))).isFalse();
  }

  @Test
  void creativeClientWaitsForServerBackedMenu() {
    assertThat(CreativeSlotItem.shouldHandleStackInteraction(player(true, true, 4))).isFalse();
  }

  private static Player player(boolean clientSide, boolean creative, int containerId) {
    Player player = mock(Player.class);
    Level level = mock(Level.class);
    when(player.level()).thenReturn(level);
    when(level.isClientSide()).thenReturn(clientSide);
    when(player.isCreative()).thenReturn(creative);
    player.containerMenu = new TestMenu(containerId);
    return player;
  }

  private static class TestMenu extends AbstractContainerMenu {
    TestMenu(int containerId) {
      super(null, containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
      return true;
    }
  }
}

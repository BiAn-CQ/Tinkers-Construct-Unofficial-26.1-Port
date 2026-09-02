package slimeknights.tconstruct.tables.menu.module;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;

import javax.annotation.Nullable;

public class SideInventoryContainer<TILE extends BlockEntity> extends BaseContainerMenu<TILE> {

  @Getter
  private final int columns;
  @Getter
  private final int slotCount;

  protected final ResourceHandler<ItemResource> itemHandler;

  public SideInventoryContainer(MenuType<?> containerType, int windowId, Inventory inv, @Nullable TILE tile, int x, int y, int columns) {
    this(containerType, windowId, inv, tile, null, x, y, columns);
  }

  public SideInventoryContainer(MenuType<?> containerType, int windowId, Inventory inv, @Nullable TILE tile, @Nullable Direction inventoryDirection, int x, int y, int columns) {
    super(containerType, windowId, inv, tile);

    // must have a TE
    if (tile == null) {
      this.itemHandler = EmptyResourceHandler.instance();
    } else {
      ResourceHandler<ItemResource> capability = tile.getLevel().getCapability(Capabilities.Item.BLOCK, tile.getBlockPos(), inventoryDirection);
      this.itemHandler = capability == null ? EmptyResourceHandler.instance() : capability;
    }

    // slot properties
    ResourceHandler<ItemResource> handler = itemHandler;
    this.slotCount = handler.size();
    // Empty/temporarily invalid multiblocks can expose zero slots. Keep layout math valid.
    this.columns = Math.max(1, columns);
    int rows = this.slotCount / this.columns;
    if (this.slotCount % this.columns != 0) {
      rows++;
    }

    // add slots
    int index = 0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < this.columns; c++) {
        if (index >= this.slotCount) {
          break;
        }

        this.addSlot(this.createSlot(handler, index, x + c * 18, y + r * 18));
        index++;
      }
    }
  }

  /**
   * Creates a slot for this inventory
   * @param itemHandler  Item handler
   * @param index        Slot index
   * @param x            Slot X position
   * @param y            Slot Y position
   * @return  Inventory slot
   */
  protected Slot createSlot(ResourceHandler<ItemResource> itemHandler, int index, int x, int y) {
    return new SmartItemHandlerSlot(itemHandler, index, x, y);
  }
}

package slimeknights.tconstruct.tables.block.entity.chest;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.tables.block.ChestBlock;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shared base logic for all Tinkers' chest tile entities */
public abstract class AbstractChestBlockEntity extends NameableBlockEntity {
  @Getter
  private final IChestItemHandler itemHandler;
  protected AbstractChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, IChestItemHandler itemHandler) {
    super(type, pos, state, name);
    itemHandler.setParent(this);
    this.itemHandler = itemHandler;
  }

  public ResourceHandler<ItemResource> getItemCapability() {
    return itemHandler;
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new TinkerChestContainerMenu(menuId, playerInventory, this);
  }

  /**
   * Checks if the given item should be inserted into the chest on interact
   * @param player    Player inserting
   * @param heldItem  Stack to insert
   * @return  Return true
   */
  public boolean canInsert(Player player, ItemStack heldItem) {
    return true;
  }

  /**
   * Minecraft 26.1 removes block entities before the old block removal hook can read their inventory.
   * Forward the pre-removal callback to the chest block so each chest keeps its configured drop policy.
   */
  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (level != null && state.getBlock() instanceof ChestBlock chest) {
      chest.dropInventoryItems(state, level, pos, itemHandler);
    }
    super.preRemoveSideEffects(pos, state);
  }

  @Override
  public void saveAdditional(CompoundTag tags) {
    super.saveAdditional(tags);
    ProblemReporter.Collector reporter = new ProblemReporter.Collector();
    TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
    itemHandler.serialize(output);
    CompoundTag handlerNBT = output.buildResult();
    tags.put(StacksResourceHandler.VALUE_IO_KEY, handlerNBT.getListOrEmpty(StacksResourceHandler.VALUE_IO_KEY));
  }

  /** Reads the inventory from NBT */
  public void readInventory(CompoundTag tags, net.minecraft.core.HolderLookup.Provider provider) {
    CompoundTag handlerNBT = new CompoundTag();
    handlerNBT.put(StacksResourceHandler.VALUE_IO_KEY, tags.getListOrEmpty(StacksResourceHandler.VALUE_IO_KEY));
    ProblemReporter.Collector reporter = new ProblemReporter.Collector();
    itemHandler.deserialize(TagValueInput.create(reporter, provider, handlerNBT));
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    readInventory(tags, registries);
  }
}

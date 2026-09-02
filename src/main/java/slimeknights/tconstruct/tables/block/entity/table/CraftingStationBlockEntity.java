package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.shared.inventory.ConfigurableContainerResourceHandler;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.CraftingContainerWrapper;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;
import slimeknights.tconstruct.tables.network.UpdateCraftingRecipePacket;

import javax.annotation.Nullable;
import java.util.Collections;

public class CraftingStationBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
  public static final Component UNCRAFTABLE = TConstruct.makeTranslation("gui", "crafting_station.uncraftable");
  private static final Component NAME = TConstruct.makeTranslation("gui", "crafting_station");

  /** Last crafted crafting recipe */
  @Nullable
  private RecipeHolder<CraftingRecipe> lastRecipe;
  /** Result inventory, lazy loads results */
  @Getter
  private final LazyResultContainer craftingResult;
  /** Crafting inventory for the recipe calls */
  private final CraftingContainerWrapper craftingInventory;

  public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.craftingStationTile.get(), pos, state, NAME, 9);
    this.itemHandler = new ConfigurableContainerResourceHandler(this, false, false);
    this.craftingInventory = new CraftingContainerWrapper(this, 3, 3);
    this.craftingResult = new LazyResultContainer(this);
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new CraftingStationContainerMenu(menuId, playerInventory, this);
  }

  @Override
  public AABB getRenderBoundingBox() {
    BlockPos maxExclusive = worldPosition.offset(1, 2, 1);
    return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), maxExclusive.getX(), maxExclusive.getY(), maxExclusive.getZ());
  }

  /* Crafting */

  @Override
  public ItemStack calcResult(@Nullable Player player) {
    if (this.level == null || isEmpty()) {
      return ItemStack.EMPTY;
    }
    // assume empty unless we learn otherwise
    ItemStack result = ItemStack.EMPTY;
    if (!this.level.isClientSide() && this.level.getServer() != null) {
      RecipeManager manager = this.level.getServer().getRecipeManager();

      // first, try the cached recipe
      CommonHooks.setCraftingPlayer(player);
      CraftingInput input = craftingInventory.asCraftingInput();
      RecipeHolder<CraftingRecipe> recipeHolder = lastRecipe;
      // if it does not match, find a new recipe
      // note we intentionally have no player access during matches, that could lead to an unstable recipe
      if (recipeHolder == null || !recipeHolder.value().matches(input, this.level)) {
        recipeHolder = manager.getRecipeFor(RecipeType.CRAFTING, input, this.level).orElse(null);
      }

      // if we have a recipe, fetch its result
      if (recipeHolder != null) {
        result = recipeHolder.value().assemble(input);

        // sync if the recipe is different
        if (recipeHolder != lastRecipe) {
          this.lastRecipe = recipeHolder;
          this.syncToRelevantPlayers(this::syncRecipe);
        }
      }
      CommonHooks.setCraftingPlayer(null);
    }
    else if (this.lastRecipe != null && this.lastRecipe.value().matches(this.craftingInventory.asCraftingInput(), this.level)) {
      CommonHooks.setCraftingPlayer(player);
      result = this.lastRecipe.value().assemble(this.craftingInventory.asCraftingInput());
      CommonHooks.setCraftingPlayer(null);
    }
    return result;
  }

  /**
   * Gets the player sensitive crafting result, also validating the player has access to this recipe
   * @param player  Player
   * @return  Player sensitive result
   */
  public ItemStack getResultForPlayer(Player player) {
    CommonHooks.setCraftingPlayer(player);
    RecipeHolder<CraftingRecipe> recipeHolder = this.lastRecipe;
    CraftingInput input = craftingInventory.asCraftingInput();

    // try matches again now that we have player access
    if (recipeHolder == null || this.level == null || !recipeHolder.value().matches(input, level)) {
      CommonHooks.setCraftingPlayer(null);
      return ItemStack.EMPTY;
    }

    // check if the player has access to the recipe, if not give up
    // Disabled because this is an absolute mess of logic, and the gain is rather small, treating this like a furnace instead
    // note the gamerule is client side only anyways, so you would have to sync it, such as in the container
    // if you want limited crafting, disable the crafting station, the design of the station is incompatible with the game rule and vanilla syncing
//    if (!recipe.isDynamic() && world.getGameRules().getBooleanOr(GameRules.DO_LIMITED_CRAFTING, false)) {
//      // mojang, why can't PlayerEntity just have a RecipeBook getter, why must I go through the sided classes? grr
//      boolean locked;
//      if (!world.isRemote) {
//        locked = player instanceof ServerPlayerEntity && !((ServerPlayerEntity) player).getRecipeBook().isUnlocked(recipe);
//      } else {
//        locked = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> player instanceof ClientPlayerEntity && !((ClientPlayerEntity) player).getRecipeBook().isUnlocked(recipe));
//      }
//      // if the player cannot craft this, block crafting
//      if (locked) {
//        CommonHooks.setCraftingPlayer(null);
//        return ItemStack.EMPTY;
//      }
//    }

    ItemStack result = recipeHolder.value().assemble(input);
    CommonHooks.setCraftingPlayer(null);
    return result;
  }

  /**
   * Removes the result from this inventory, updating inputs and triggering recipe hooks
   * @param player  Player taking result
   * @param result  Result removed
   * @param amount  Number of times crafted
   */
  public void takeResult(Player player, ItemStack result, int amount) {
    RecipeHolder<CraftingRecipe> recipeHolder = this.lastRecipe;
    if (recipeHolder == null || this.level == null) {
      return;
    }
    CraftingRecipe recipe = recipeHolder.value();
    CraftingInput.Positioned positionedInput = craftingInventory.asPositionedCraftingInput();
    CraftingInput input = positionedInput.input();

    // fire crafting events
    if (!recipe.isSpecial()) {
      // unlock the recipe if it was not unlocked, so it shows in the recipe book
      player.awardRecipes(Collections.singleton(recipeHolder));
    }
    result.onCraftedBy(player, amount);
    EventHooks.firePlayerCraftingEvent(player, result, this.craftingInventory);

    // update all slots in the inventory
    // remove remaining items
    CommonHooks.setCraftingPlayer(player);
    NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
    CommonHooks.setCraftingPlayer(null);
    int left = positionedInput.left();
    int top = positionedInput.top();
    for (int row = 0; row < input.height(); row++) {
      for (int column = 0; column < input.width(); column++) {
        int inputIndex = column + row * input.width();
        int slot = column + left + (row + top) * craftingInventory.getWidth();
        ItemStack original = this.getItem(slot);
        ItemStack newStack = remaining.get(inputIndex);

        // if empty or size 1, set directly (decreases by 1)
        if (original.isEmpty() || original.getCount() == 1) {
          this.setItem(slot, newStack);
        }
        else if (ItemStack.isSameItemSameComponents(original, newStack)) {
          // if matching, merge (decreasing by 1)
          newStack.grow(original.getCount() - 1);
          this.setItem(slot, newStack);
        }
        else {
          // directly update the slot
          this.setItem(slot, slimeknights.tconstruct.library.utils.ItemStackDataUtil.copyStackWithSize(original, original.getCount() - 1));
          // otherwise, drop the item as the player
          if (!newStack.isEmpty() && !player.getInventory().add(newStack)) {
            player.drop(newStack, false);
          }
        }
      }
    }
  }

  /** Sends a message alerting the player this item is currently uncraftable, typically due to gamerules */
  public void notifyUncraftable(Player player) {
    // if empty, send a message so the player is more aware of why they cannot craft it, sent to chat as status bar is not visible
    // TODO: consider moving into the UI somewhere
    if (level != null && !level.isClientSide()) {
      player.sendSystemMessage(CraftingStationBlockEntity.UNCRAFTABLE);
    }
  }

  @Override
  public void onCraft(Player player, ItemStack result, int amount) {
    // update the inputs and trigger recipe hooks
    if (amount != 0 && !result.isEmpty()) {
      takeResult(player, result, amount);
    }
  }

  @Override
  public void setItem(int slot, ItemStack itemstack) {
    super.setItem(slot, itemstack);
    // clear the crafting result when the matrix changes so we recalculate the result
    this.craftingResult.clearContent();
  }


  /* Syncing */

  /**
   * Sends the current recipe to the given player
   * @param player  Player to send an update to
   */
  public void syncRecipe(Player player) {
    // must have a last recipe and a server world
    if (this.lastRecipe != null && this.level != null && !this.level.isClientSide() && player instanceof ServerPlayer) {
      TinkerNetwork.getInstance().sendTo(new UpdateCraftingRecipePacket(this.worldPosition, this.lastRecipe), (ServerPlayer) player);
    }
  }

  /**
   * Updates the recipe from the server
   * @param recipe  New recipe
   */
  public void updateRecipe(RecipeHolder<CraftingRecipe> recipe) {
    this.lastRecipe = recipe;
    this.craftingResult.clearContent();
  }
}

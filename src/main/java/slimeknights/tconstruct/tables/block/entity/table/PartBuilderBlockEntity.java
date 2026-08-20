package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.shared.inventory.ConfigurableInvWrapperCapability;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.block.entity.inventory.PartBuilderContainerWrapper;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;
import slimeknights.tconstruct.tables.network.UpdatePartBuilderRecipesPacket;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PartBuilderBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
  /** First slot containing materials */
  public static final int MATERIAL_SLOT = 0;
  /** Second slot containing the patterns */
  public static final int PATTERN_SLOT = 1;
  /** Title for the GUI */
  private static final Component NAME = TConstruct.makeTranslation("gui", "part_builder");

  /** Result inventory, lazy loads results */
  @Getter
  private final LazyResultContainer craftingResult;
  /** Crafting inventory for the recipe calls */
  @Getter
  private final PartBuilderContainerWrapper inventoryWrapper;
  /* Current buttons to display */
  @Nullable
  private Map<Pattern,IPartBuilderRecipe> recipes = null;
  /** Server recipe holders retained so the input-sensitive list can be synced to the client. */
  @Nullable
  private Map<Pattern,RecipeHolder<IPartBuilderRecipe>> recipeHolders = null;
  @Nullable
  private List<Pattern> sortedButtons = null;
  /** Currently selected recipe index */
  private Pattern selectedPattern = null;
  /** Index of the currently selected pattern */
  private int selectedPatternIndex = -2;

  public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.partBuilderTile.get(), pos, state, NAME, 2);
    this.itemHandler = new ConfigurableInvWrapperCapability(this, false, false);
    this.inventoryWrapper = new PartBuilderContainerWrapper(this);
    this.craftingResult = new LazyResultContainer(this);
  }

  /**
   * Gets a map of all recipes for the current inputs
   * @return  List of recipes for the current inputs
   */
  protected Map<Pattern,IPartBuilderRecipe> getCurrentRecipes() {
    if (level == null) {
      return Collections.emptyMap();
    }
    if (recipes == null) {
      // no recipes if we lack a pattern
      if (getItem(PATTERN_SLOT).isEmpty()) {
        recipes = Collections.emptyMap();
        recipeHolders = Collections.emptyMap();
        sortedButtons = Collections.emptyList();
      } else {
        record PatternRecipe(Pattern pattern, RecipeHolder<IPartBuilderRecipe> holder) {}
        // fetch all recipes that can match these inputs, the map ensures the patterns are unique
        recipeHolders = slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getAllRecipesFor(slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getRecipeManager(level),TinkerRecipeTypes.PART_BUILDER.get()).stream()
                       .filter(holder -> holder.value().partialMatch(inventoryWrapper))
                       .sorted(Comparator.comparing(RecipeHolder::id))
                       .flatMap(holder -> holder.value().getPatterns(inventoryWrapper).map(p -> new PatternRecipe(p, holder)))
                       .collect(Collectors.toMap(PatternRecipe::pattern, PatternRecipe::holder, (a, b) -> a));
        recipes = recipeHolders.entrySet().stream()
                       .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().value(), (a, b) -> a));
        sortedButtons = recipes.entrySet()
                               .stream()
                               .sorted(Comparator.<Entry<Pattern,IPartBuilderRecipe>>comparingInt(ent -> ent.getValue().getCost()).thenComparing(Entry::getKey))
                               .map(Entry::getKey).collect(Collectors.toList());
      }
    }
    return recipes;
  }

  /** Gets the server-filtered recipe entries needed by the client screen. */
  public List<UpdatePartBuilderRecipesPacket.RecipeEntry> getCurrentRecipeEntries() {
    if (level == null || level.isClientSide()) {
      return Collections.emptyList();
    }
    getCurrentRecipes();
    if (recipeHolders == null || recipeHolders.isEmpty()) {
      return Collections.emptyList();
    }
    return recipeHolders.entrySet().stream()
      .map(entry -> new UpdatePartBuilderRecipesPacket.RecipeEntry(entry.getKey(), entry.getValue()))
      .toList();
  }

  /** Gets the list of sorted buttons */
  public List<Pattern> getSortedButtons() {
    if (level == null) {
      return Collections.emptyList();
    }
    if (sortedButtons == null) {
      getCurrentRecipes();
    }
    return sortedButtons;
  }

  /** Gets the index of the selected pattern */
  public int getSelectedIndex() {
    if (selectedPatternIndex == -2) {
      if (selectedPattern != null) {
        selectedPatternIndex = getSortedButtons().indexOf(selectedPattern);
      } else {
        selectedPatternIndex = -1;
      }
    }
    return selectedPatternIndex;
  }

  /**
   * Gets the currently selected recipe
   * @return  Selected recipe, or null if invalid or no recipe
   */
  @Nullable
  public IPartBuilderRecipe getPartRecipe() {
    if (selectedPattern != null) {
      return getCurrentRecipes().get(selectedPattern);
    }
    return null;
  }

  /** Gets the first available recipe */
  @Nullable
  public IPartBuilderRecipe getFirstRecipe() {
    List<Pattern> sortedButtons = getSortedButtons();
    if (sortedButtons.isEmpty()) {
      return null;
    }
    return getCurrentRecipes().get(sortedButtons.get(0));
  }

  /**
   * Gets the material recipe for the material slot
   * @return  Material slot
   */
  @Nullable
  public IMaterialValue getMaterialRecipe() {
    return inventoryWrapper.getMaterial();
  }

  /** If true, hides the uncraftable error message on the screen */
  public boolean allowUncraftable() {
    // if a recipe is selected, its behavior dictates the message
    IPartBuilderRecipe recipe = getPartRecipe();
    if (recipe != null) {
      return recipe.allowUncraftable();
    }
    // otherwise, if any button says its allowed then its allowed
    for (IPartBuilderRecipe button : getCurrentRecipes().values()) {
      if (button.allowUncraftable()) {
        return true;
      }
    }
    // no button says its allowed? then its not
    return false;
  }

  /**
   * Refreshes the current recipe
   * @param refreshRecipeList  If true, refreshes the full recipe list too
   */
  private void refresh(boolean refreshRecipeList) {
    if (refreshRecipeList) {
      this.recipes = null;
      this.recipeHolders = null;
      this.sortedButtons = null;
    }
    this.selectedPatternIndex = -2;
    this.craftingResult.clearContent();
    // update screen display
    if (refreshRecipeList) {
      syncToRelevantPlayers(this::syncRecipes);
      syncScreenToRelevantPlayers();
    }
  }

  /**
   * Selects a recipe in the table
   * @param pattern  New pattern
   */
  public void selectRecipe(@Nullable Pattern pattern) {
    if (pattern != null && getCurrentRecipes().containsKey(pattern)) {
      selectedPattern = pattern;
    } else {
      selectedPattern = null;
    }
    refresh(false);
  }

  /**
   * Selects a pattern by index
   * @param index  New index
   */
  public void selectRecipe(int index) {
    if (index < 0) {
      selectedPattern = null;
    } else {
      List<Pattern> list = getSortedButtons();
      if (index < list.size()) {
        selectedPattern = list.get(index);
      } else {
        selectedPattern = null;
      }
    }
    refresh(false);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    ItemStack original = getItem(slot);
    super.setItem(slot, stack);
    if (slot == MATERIAL_SLOT) {
      // if item or NBT changed, update
      if (!ItemStack.isSameItemSameComponents(original, stack)) {
        this.inventoryWrapper.refreshMaterial();
        refresh(true);
        // if size changed, we are still the same material but might no longer have enough
        // same stack calling this method typically indicates a size change, stacks being mutable is annoying
      } else if (original.getCount() != stack.getCount() || original == stack) {
        this.craftingResult.clearContent();
        syncScreenToRelevantPlayers();
      }
      // any other slot, only an item change means update
    } else if (original.getItem() != stack.getItem()) {
      refresh(true);
    }
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    PartBuilderContainerMenu menu = new PartBuilderContainerMenu(menuId, playerInventory, this);
    syncRecipes(playerEntity);
    return menu;
  }

  /** Sends the current input-sensitive recipe and material state to one viewer. */
  private void syncRecipes(Player player) {
    if (this.level != null && !this.level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
      TinkerNetwork.getInstance().sendTo(
        new UpdatePartBuilderRecipesPacket(this.worldPosition, getCurrentRecipeEntries(), inventoryWrapper.getMaterial()),
        serverPlayer);
    }
  }

  /** Applies the server-filtered state on the client. */
  public void updateRecipes(List<UpdatePartBuilderRecipesPacket.RecipeEntry> entries,
                            @Nullable UpdatePartBuilderRecipesPacket.MaterialData material) {
    if (this.level == null || !this.level.isClientSide()) {
      return;
    }
    Pattern previousPattern = this.selectedPattern;
    this.recipeHolders = entries.stream().collect(Collectors.toMap(
      UpdatePartBuilderRecipesPacket.RecipeEntry::pattern,
      UpdatePartBuilderRecipesPacket.RecipeEntry::recipe,
      (a, b) -> a));
    this.recipes = this.recipeHolders.entrySet().stream()
      .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().value(), (a, b) -> a));
    this.sortedButtons = this.recipes.entrySet().stream()
      .sorted(Comparator.<Entry<Pattern,IPartBuilderRecipe>>comparingInt(ent -> ent.getValue().getCost()).thenComparing(Entry::getKey))
      .map(Entry::getKey)
      .toList();
    this.selectedPattern = previousPattern != null && this.recipes.containsKey(previousPattern) ? previousPattern : null;
    this.selectedPatternIndex = -2;
    if (material == null) {
      this.inventoryWrapper.setSyncedMaterial(null, 0, 1, ItemStack.EMPTY);
    } else {
      this.inventoryWrapper.setSyncedMaterial(material.material(), material.value(), material.needed(), material.leftover());
    }
    this.craftingResult.clearContent();
  }

  @Override
  public ItemStack calcResult(@Nullable Player player) {
    if (level != null) {
      IPartBuilderRecipe recipe = getPartRecipe();
      if (recipe != null && recipe.matches(inventoryWrapper, level)) {
        return recipe.assemble(inventoryWrapper, level.registryAccess(), selectedPattern);
      }
    }
    return ItemStack.EMPTY;
  }

  /**
   * Shrinks the given slot
   * @param slot    Slot
   * @param amount  Amount to shrink
   */
  private void shrinkSlot(int slot, int amount, Player player) {
    if (amount <= 0) {
      return;
    }
    ItemStack stack = getItem(slot);
    if (!stack.isEmpty()) {
      var remainder = stack.getCraftingRemainder();
      ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
      if (amount > 1) {
        container.setCount(container.getCount() * amount);
      }
      if (stack.getCount() <= amount) {
        setItem(slot, container);
      } else {
        stack.shrink(amount);
        ItemHandlerHelper.giveItemToPlayer(player, container);
      }
    }
  }

  @Override
  public void onCraft(Player player, ItemStack result, int amount) {
    if (amount == 0 || this.level == null) {
      return;
    }
    // the recipe should match if we got this far, but being null is a problem
    IPartBuilderRecipe recipe = getPartRecipe();
    if (recipe == null) {
      return;
    }

    // we are definitely crafting at this point
    result.onCraftedBy(player, amount);
    EventHooks.firePlayerCraftingEvent(player, result, this.inventoryWrapper);
    this.playCraftSound(player);

    // give the player any leftovers
    if (level != null && !level.isClientSide()) {
      ItemStack leftover = recipe.getLeftover(inventoryWrapper, selectedPattern);
      if (!leftover.isEmpty()) {
        ItemHandlerHelper.giveItemToPlayer(player, leftover);
      }
    }

    // shrink the inputs
    shrinkSlot(MATERIAL_SLOT, recipe.getItemsUsed(inventoryWrapper), player);
    if (!getItem(PATTERN_SLOT).is(TinkerTags.Items.REUSABLE_PATTERNS)) {
      shrinkSlot(PATTERN_SLOT, 1, player);
    }

    // sync display, mainly for the material value
    syncScreenToRelevantPlayers();
  }
}

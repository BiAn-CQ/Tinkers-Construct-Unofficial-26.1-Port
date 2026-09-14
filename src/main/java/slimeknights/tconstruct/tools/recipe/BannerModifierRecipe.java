package slimeknights.tconstruct.tools.recipe;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.BannerModule;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Recipe to add a banner to a shield */
public class BannerModifierRecipe implements ITinkerStationRecipe, IMultiRecipe<IDisplayModifierRecipe> {
  public static final slimeknights.mantle.data.loadable.record.RecordLoadable<BannerModifierRecipe> LOADER = slimeknights.mantle.data.loadable.record.RecordLoadable.create(
    slimeknights.mantle.data.loadable.field.ContextKey.ID.requiredField(),
    slimeknights.mantle.data.loadable.common.IngredientLoadable.ALLOW_EMPTY.defaultField("clear_input", slimeknights.tconstruct.library.recipe.TinkerIngredients.EMPTY, false, r -> r.clearInput),
    BannerModifierRecipe::new);
  private final net.minecraft.world.item.crafting.Ingredient clearInput;
  @Getter
  private final Identifier id;

  public Identifier getId() {
    return id;
  }

  public BannerModifierRecipe() {
    this(TConstruct.getResource("banner_modifier"));
  }

  public BannerModifierRecipe(Identifier id) {
    this(id, slimeknights.tconstruct.library.recipe.TinkerIngredients.EMPTY);
  }

  public BannerModifierRecipe(Identifier id, net.minecraft.world.item.crafting.Ingredient clearInput) {
    this.id = id;
    this.clearInput = clearInput;
    ModifierRecipeLookup.addRecipeModifier(null, TinkerModifiers.banner);
  }

  @Override
  public boolean matches(ITinkerStationContainer inv, Level world) {
    // ensure this modifier can be applied
    if (!inv.getTinkerableStack().is(TinkerTags.Items.BANNER)) {
      return false;
    }
    // slots must be only banner
    boolean found = false;
    boolean clear = false;
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack input = inv.getInput(i);
      if (!input.isEmpty()) {
        if (slimeknights.tconstruct.library.recipe.TinkerIngredients.matches(clearInput, input)) {
          if (clear) return false;
          clear = true;
        } else if (input.getItem() instanceof BannerItem) {
          if (found) {
            // multiple banners
            return false;
          }
          found = true;
        } else {
          // non-banner input
          return false;
        }
      }
    }
    return found;
  }

  @Override
  public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer inv, HolderLookup.Provider access) {
    ToolStack tool = inv.getTinkerable().copy();

    ModDataNBT persistentData = tool.getPersistentData();
    ModifierId key = TinkerModifiers.banner.getId();

    // locate the banner
    ItemStack banner = ItemStack.EMPTY;
    DyeColor dye = DyeColor.BLACK;
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack stack = inv.getInput(i);
      if (!stack.isEmpty() && stack.getItem() instanceof BannerItem bannerItem) {
        banner = stack;
        dye = bannerItem.getColor();
        // only need 1
        break;
      }
    }

    // should never happen
    if (banner.isEmpty()) {
      return RecipeResult.pass();
    }

    // get the banner data
    BannerPatternLayers patterns = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
    for (int i = 0; i < inv.getInputCount(); i++) {
      if (!inv.getInput(i).isEmpty() && slimeknights.tconstruct.library.recipe.TinkerIngredients.matches(clearInput, inv.getInput(i))) {
        if (patterns.layers().isEmpty()) {
          return RecipeResult.failure(TConstruct.makeTranslationKey("recipe", "banner.clear.no_patterns"));
        }
        dye = null;
        break;
      }
    }

    // apply the pattern
    BannerModule.copyPatterns(tool.getPersistentData(), key, dye, patterns);

    // add the modifier if missing
    if (tool.getModifierLevel(key) == 0) {
      tool.addModifier(key, 1);
    }
    return ITinkerStationRecipe.success(tool, inv);
  }

  @Override
  public RecipeSerializer getSerializer() {
    return TinkerModifiers.bannerModifierSerializer.get();
  }


  /* JEI */

  @Nullable
  private List<IDisplayModifierRecipe> displayRecipes;

  @Override
  public List<IDisplayModifierRecipe> getRecipes(HolderLookup.Provider access) {
    if (displayRecipes == null) {
      List<ItemStack> toolInputs = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, TinkerTags.Items.BANNER)
        .map(item -> {
          ItemStack stack = IModifiableDisplay.getDisplayStack(item);
          if (stack.getMaxStackSize() > 1) {
            stack = stack.copyWithCount(Math.min(stack.getMaxStackSize(), DEFAULT_TOOL_STACK_SIZE));
          }
          return stack;
      }).toList();
      if (!toolInputs.isEmpty()) {
        Identifier id = getId();
        Stream<IDisplayModifierRecipe> recipes = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.BANNERS)
          .flatMap(item -> {
            if (item instanceof BannerItem banner) {
              return Stream.of(new DisplayRecipe(id, toolInputs, banner.getColor(), List.of(new ItemStack(banner)), List.of(), BannerPatternLayers.EMPTY));
            }
            return Stream.empty();
          });
        // If a clear ingredient exists, add a separate JEI recipe showing the clear operation.
        if (clearInput != slimeknights.tconstruct.library.recipe.TinkerIngredients.EMPTY) {
          BannerPatternLayers samplePatterns = createSamplePatterns(access);
          List<ItemStack> patternedBanners = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.BANNERS)
            .map(item -> {
              ItemStack stack = new ItemStack(item);
              stack.set(DataComponents.BANNER_PATTERNS, samplePatterns);
              return stack;
            }).toList();
          recipes = Stream.concat(recipes, Stream.of(new DisplayRecipe(
            id, toolInputs, null, patternedBanners, List.of(slimeknights.tconstruct.library.recipe.TinkerIngredients.getItems(clearInput)), samplePatterns
          )));
        }
        displayRecipes = recipes.collect(Collectors.toList());
      } else {
        displayRecipes = List.of();
      }
    }
    return displayRecipes;
  }

  /** Creates one visible pattern for the JEI clear-banner example. */
  private static BannerPatternLayers createSamplePatterns(HolderLookup.Provider access) {
    Holder<BannerPattern> cross = access.lookupOrThrow(Registries.BANNER_PATTERN).get(BannerPatterns.CROSS).orElse(null);
    if (cross == null) {
      return BannerPatternLayers.EMPTY;
    }
    return new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(cross, DyeColor.BLACK)));
  }

  /** Display recipe instance */
  private static class DisplayRecipe implements IDisplayModifierRecipe {
    private static final IntRange LEVELS = new IntRange(1, 1);
    private final ModifierEntry RESULT = new ModifierEntry(TinkerModifiers.banner, 1);

    @Getter
    private final Identifier recipeId;
    private final List<ItemStack> banner;
    private final List<ItemStack> clearInput;
    @Getter
    private final List<ItemStack> toolWithoutModifier;
    @Getter
    private final List<ItemStack> toolWithModifier;
    @Getter
    private final Component variant;
    private final DyeColor dye;
    private final BannerPatternLayers patterns;
    public DisplayRecipe(Identifier recipeId, List<ItemStack> tools, @Nullable DyeColor dye, List<ItemStack> banner,
                         List<ItemStack> clearInput, BannerPatternLayers patterns) {
      this.recipeId = recipeId;
      this.toolWithoutModifier = tools;
      this.banner = banner;
      this.clearInput = clearInput;
      this.dye = dye;
      this.variant = dye != null ? Component.translatable("color.minecraft." + dye.getSerializedName()) : TConstruct.makeTranslation("recipe", "banner.clear");
      this.patterns = patterns;

      ModifierId key = RESULT.getId();
      List<ModifierEntry> results = List.of(RESULT);
      toolWithModifier = tools.stream().map(stack -> IDisplayModifierRecipe.withModifiers(stack, DEFAULT_TOOL_STACK_SIZE, results, data -> BannerModule.copyPatterns(data, key, dye, patterns))).toList();
    }

    @Override
    public ModifierEntry getDisplayResult() {
      return RESULT;
    }

    @Override
    public int getInputCount() {
      return clearInput.isEmpty() ? 1 : 2;
    }

    @Override
    public List<ItemStack> getDisplayItems(int slot) {
      if (slot == 0) {
        return banner;
      }
      if (slot == 1) {
        return clearInput;
      }
      return List.of();
    }

    @Override
    public IntRange getLevel() {
      return LEVELS;
    }

    @Override
    public boolean isTool(ItemStack check) {
      return check.is(TinkerTags.Items.BANNER);
    }

    @Nullable
    @Override
    public Component canApply(slimeknights.tconstruct.library.tools.nbt.IToolStackView tool) {
      return null;
    }

    @Override
    public void applyModifier(ToolStack tool) {
      ModifierId modifier = TinkerModifiers.banner.getId();
      BannerModule.copyPatterns(tool.getPersistentData(), modifier, dye, patterns);
      if (tool.getModifierLevel(modifier) == 0) {
        tool.addModifier(modifier, 1);
      }
    }

    @Override
    public boolean shouldDisplayValidate() {
      return false;
    }
  }
}

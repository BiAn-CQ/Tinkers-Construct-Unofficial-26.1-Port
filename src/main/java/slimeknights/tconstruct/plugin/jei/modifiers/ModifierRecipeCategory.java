package slimeknights.tconstruct.plugin.jei.modifiers;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;

import javax.annotation.Nullable;

/** Category for display recipes for crafting {@link ModifierEntry}. */
public class ModifierRecipeCategory extends AbstractTinkerStationCategory<IDisplayModifierRecipe> {
  protected static final Identifier BACKGROUND_LOC = AbstractTinkerStationCategory.BACKGROUND_LOC;
  private static final Component TITLE = TConstruct.makeTranslation("jei", "modifiers.title");

  // translation
  private static final Component TEXT_INCREMENTAL = TConstruct.makeTranslation("jei", "modifiers.incremental");
  private static final String KEY_MIN = TConstruct.makeTranslationKey("jei", "modifiers.level.min");
  private static final String KEY_MAX = TConstruct.makeTranslationKey("jei", "modifiers.level.max");
  private static final String KEY_RANGE = TConstruct.makeTranslationKey("jei", "modifiers.level.range");
  private static final String KEY_EXACT = TConstruct.makeTranslationKey("jei", "modifiers.level.exact");

  /** Draws the slotless input icon. */
  private static final IDrawable SLOTLESS = new IDrawable() {
    @Override
    public int getWidth() {
      return SlotIngredientRenderer.INPUT.getWidth();
    }

    @Override
    public int getHeight() {
      return SlotIngredientRenderer.INPUT.getHeight();
    }

    @Override
    public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
      graphics.pose().pushMatrix();
      graphics.pose().translate((float)xOffset, (float)yOffset);
      SlotIngredientRenderer.INPUT.render(graphics, null);
      graphics.pose().popMatrix();
    }
  };

  private final ModifierIngredientRenderer modifierRenderer = new ModifierIngredientRenderer(124, 10);
  private final IDrawable requirements, incremental;

  public ModifierRecipeCategory(IGuiHelper helper) {
    super(helper, TConstructJEIConstants.MODIFIERS, TITLE,
      helper.createDrawableItemStack(CreativeSlotItem.withSlot(new ItemStack(TinkerModifiers.creativeSlotItem), SlotType.UPGRADE)));
    this.requirements = helper.createDrawable(BACKGROUND_LOC, 128, 17, 16, 16);
    this.incremental = helper.createDrawable(BACKGROUND_LOC, 128, 33, 16, 16);
  }

  /** Gets the text describing the required modifier level, if any. */
  @Override
  @Nullable
  protected Component getVariantText(IDisplayModifierRecipe recipe) {
    Component variant = recipe.getVariant();
    if (variant != null) {
      return variant;
    }
    IntRange level = recipe.getLevel();
    int min = level.min();
    int max = level.max();
    if (min == 1) {
      if (max < ModifierEntry.VALID_LEVEL.max()) {
        return Component.translatable(KEY_MAX, max);
      }
    } else if (min == max) {
      return Component.translatable(KEY_EXACT, min);
    } else if (max == ModifierEntry.VALID_LEVEL.max()) {
      return Component.translatable(KEY_MIN, min);
    } else {
      return Component.translatable(KEY_RANGE, min, max);
    }
    return null;
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, IDisplayModifierRecipe recipe, IFocusGroup focuses) {
    super.createRecipeExtras(builder, recipe, focuses);

    ModifierEntry result = recipe.getDisplayResult();
    Component requirementsError = result.getHook(ModifierHooks.REQUIREMENTS).requirementsError(result);
    if (requirementsError != null) {
      builder.addDrawable(requirements).setPosition(66, 58);
    }
    if (recipe.isIncremental()) {
      builder.addDrawable(incremental).setPosition(83, 59);
    }
    if (recipe.getSlots() == null) {
      builder.addDrawable(SLOTLESS).setPosition(102, 58);
    }
  }

  @Override
  protected boolean isToolCatalyst(IDisplayModifierRecipe recipe) {
    return true;
  }

  @Override
  public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, IDisplayModifierRecipe recipe,
                         mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, double x, double y) {
    super.getTooltip(tooltip, recipe, slots, x, y);
    if (x >= 66 && x < 82 && y >= 58 && y < 74) {
      ModifierEntry result = recipe.getDisplayResult();
      Component error = result.getHook(ModifierHooks.REQUIREMENTS).requirementsError(result);
      if (error != null) tooltip.add(error);
    }
    if (recipe.isIncremental() && x >= 83 && x < 99 && y >= 59 && y < 75) {
      tooltip.add(TEXT_INCREMENTAL);
    }
    if (recipe.getSlots() == null && x >= 102 && x < 126 && y >= 58 && y < 74) {
      tooltip.addAll(SlotIngredientRenderer.TEXT_FREE);
    }
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, IDisplayModifierRecipe recipe, IFocusGroup focuses) {
    super.setRecipe(builder, recipe, focuses);

    // modifier output
    builder.addOutputSlot(3, 3)
      .setCustomRenderer(TConstructJEIConstants.MODIFIER_TYPE, modifierRenderer)
      .addIngredient(TConstructJEIConstants.MODIFIER_TYPE, recipe.getDisplayResult());

    // modifier slots
    SlotCount slots = recipe.getSlots();
    if (slots != null) {
      builder.addInputSlot(102, 58)
        .setCustomRenderer(TConstructJEIConstants.SLOT_TYPE, SlotIngredientRenderer.INPUT)
        .addIngredient(TConstructJEIConstants.SLOT_TYPE, slots);
    }

    // result slots are determined from the volatile data hook.
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
      .addIngredients(TConstructJEIConstants.SLOT_TYPE, recipe.getResultSlots());
  }

  /** @deprecated never needed to be called by an addon */
  @Deprecated(forRemoval = true)
  @Internal
  public static void clearSlimeskullCache() {
    AbstractTinkerStationCategory.clearLookupCache();
  }
}

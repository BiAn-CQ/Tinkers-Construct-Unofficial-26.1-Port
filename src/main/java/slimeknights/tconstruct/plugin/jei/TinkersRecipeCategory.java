package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Compatibility bridge for the 26.1 JEI category API.
 *
 * <p>JEI 15 exposed the category background directly through
 * {@code IRecipeCategory#getBackground()}. JEI 29 no longer has that method.
 * The background is therefore drawn at the beginning of each category's
 * {@code draw} method, before JEI draws the recipe slots. Adding it through
 * {@code createRecipeExtras} is not equivalent: JEI draws extras after slots,
 * which covers the ingredients.</p>
 */
public interface TinkersRecipeCategory<T> extends IRecipeCategory<T> {
  /** Background drawable used by the original Tinkers JEI category. */
  IDrawable getBackground();

  @Override
  default int getWidth() {
    return getBackground().getWidth();
  }

  @Override
  default int getHeight() {
    return getBackground().getHeight();
  }

  /**
   * Draws the original Tinkers background in the same layer where JEI 15 drew
   * the category background.
   */
  default void drawBackground(GuiGraphicsExtractor graphics) {
    getBackground().draw(graphics, 0, 0);
  }
}

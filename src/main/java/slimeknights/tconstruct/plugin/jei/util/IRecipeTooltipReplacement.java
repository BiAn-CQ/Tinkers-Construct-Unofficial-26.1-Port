package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import com.mojang.datafixers.util.Either;

import java.util.ArrayList;
import java.util.List;

/** @deprecated use {@link FluidTooltipCallback} for better handling of advanced tooltip information */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface IRecipeTooltipReplacement extends IRecipeSlotRichTooltipCallback {
  /** Tooltip replacement that keeps just the name and mod ID */
  IRecipeTooltipReplacement EMPTY = (slot, tooltip) -> {};

  @Override
  default void onRichTooltip(IRecipeSlotView recipeSlotView, ITooltipBuilder builder) {
    List<Component> lines = new ArrayList<>();
    for (Either<FormattedText, TooltipComponent> line : builder.getLines()) {
      line.left().ifPresent(text -> lines.add(text instanceof Component component ? component : Component.literal(text.getString())));
    }
    if (lines.isEmpty()) {
      return;
    }
    Component name = lines.get(0);
    List<Component> replacement = new ArrayList<>();
    replacement.add(name);
    addMiddleLines(recipeSlotView, replacement);
    builder.clear();
    builder.addAll(replacement);
  }

  /** Adds the lines between the name and mod ID */
  void addMiddleLines(IRecipeSlotView recipeSlotView, List<Component> tooltip);
}

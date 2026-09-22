package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Tooltip bounds for JEI widgets whose 26.1 API does not expose tooltip setters. */
public record TooltipArea(int x, int y, int width, int height, List<Component> lines) implements IRecipeWidget {
  @Override
  public ScreenPosition getPosition() {
    return new ScreenPosition(x, y);
  }

  @Override
  public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
    if (mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height) {
      lines.forEach(tooltip::add);
    }
  }
}

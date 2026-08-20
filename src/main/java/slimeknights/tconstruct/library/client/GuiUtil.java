package slimeknights.tconstruct.library.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

/** GUI helpers shared by the Tinker tables and smeltery screens. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuiUtil {
  /**
   * Gets the origin of the central panel. Multi-module screens expand their
   * inherited leftPos to include side panels, but their background and local
   * widgets remain relative to cornerX/cornerY.
   */
  public static int getGuiLeft(AbstractContainerScreen<?> screen) {
    return screen instanceof MultiModuleScreen<?> multi ? multi.cornerX : screen.getLeftPos();
  }

  /** Gets the origin of the central panel. */
  public static int getGuiTop(AbstractContainerScreen<?> screen) {
    return screen instanceof MultiModuleScreen<?> multi ? multi.cornerY : screen.getTopPos();
  }

  /** Draws a container background at the screen's current GUI position. */
  public static void drawBackground(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, Identifier background) {
    int left = getGuiLeft(screen);
    int top = getGuiTop(screen);
    int width = screen.getImageWidth();
    int height = screen.getImageHeight();
    if (screen instanceof MultiModuleScreen<?> multi) {
      width = multi.realWidth;
      height = multi.realHeight;
    }
    graphics.blit(RenderPipelines.GUI_TEXTURED, background, left, top,
      0, 0, width, height, 256, 256);
  }

  /** Checks if a point is inside the given rectangle. */
  public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
  }

  /** Checks if a point is inside the filled part of a vertical tank. */
  public static boolean isTankHovered(int mouseX, int mouseY, int amount, int capacity, int x, int y, int width, int height) {
    if (capacity <= 0 || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
      return false;
    }
    int topHeight = height - height * amount / capacity;
    return mouseY >= y + topHeight;
  }

  /** Renders a fluid tank with the stack amount as its current level. */
  public static void renderFluidTank(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack,
                                     int capacity, int x, int y, int width, int height, int depth) {
    renderFluidTank(graphics, screen, stack, stack.getAmount(), capacity, x, y, width, height, depth);
  }

  /** Renders a fluid tank with an explicit current amount. */
  public static void renderFluidTank(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack,
                                     int amount, int capacity, int x, int y, int width, int height, int depth) {
    if (!stack.isEmpty() && capacity > 0 && amount > 0) {
      int fluidHeight = Math.min(height * amount / capacity, height);
      renderTiledFluid(graphics, screen, stack, x, y + height - fluidHeight, width, fluidHeight, depth);
    }
  }

  /** Renders a fluid sprite over the requested area. */
  public static void renderTiledFluid(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack,
                                      int x, int y, int width, int height, int depth) {
    if (!stack.isEmpty() && width > 0 && height > 0) {
      Fluid fluid = stack.getFluid();
      // 26.1 keeps vanilla and dynamically registered fluid models in the
      // client model set.  FluidTextureManager intentionally only contains
      // Mantle's legacy fluid-texture overrides, so using its water fallback
      // for a vanilla fluid makes lava render as an unrelated grey sprite.
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.defaultFluidState());
      TextureAtlasSprite fluidSprite = Minecraft.getInstance().getAtlasManager()
        .get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, fluidModel.stillMaterial().sprite().contents().name()));
      int color = fluidModel.fluidTintSource() == null
                  ? FluidTextureManager.getColor(fluid.getFluidType())
                  : fluidModel.fluidTintSource().colorAsStack(stack);
      renderTiledTextureAtlas(graphics, screen, fluidSprite, x, y, width, height,
        color, fluid.getFluidType().isLighterThanAir());
    }
  }

  /**
   * Draws repeated atlas sprites without mutating global shader or vertex state.
   * The extraction GUI API owns the render ordering in 26.1, so each tile is
   * submitted as an independent GUI sprite.
   */
  public static void renderTiledTextureAtlas(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen,
                                             TextureAtlasSprite sprite, int x, int y, int width, int height,
                                             int color, boolean upsideDown) {
    if (width <= 0 || height <= 0) {
      return;
    }
    int startX = x + getGuiLeft(screen);
    int startY = y + getGuiTop(screen);
    int spriteWidth = Math.max(1, sprite.contents().width());
    int spriteHeight = Math.max(1, sprite.contents().height());
    for (int offsetY = 0; offsetY < height; offsetY += spriteHeight) {
      int tileHeight = Math.min(spriteHeight, height - offsetY);
      for (int offsetX = 0; offsetX < width; offsetX += spriteWidth) {
        int tileWidth = Math.min(spriteWidth, width - offsetX);
        // A GUI sprite submission is clipped/scaled by the extractor. This is
        // deliberately used for the final partial tile as well; it avoids the
        // old PoseStack/BufferBuilder path that is invalid in 26.1.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, startX + offsetX, startY + offsetY,
          tileWidth, tileHeight, color);
      }
    }
  }

  /** Draws a vertical progress bar from the bottom up. */
  public static void drawProgressUp(GuiGraphicsExtractor graphics, ElementScreen element, int x, int y, float progress) {
    int height;
    if (progress > 1) {
      height = element.h;
    } else if (progress < 0) {
      height = 0;
    } else {
      height = (int) (progress * element.h + 0.5f);
    }
    if (height > 0) {
      int deltaY = element.h - height;
      graphics.blit(RenderPipelines.GUI_TEXTURED, element.texture, x, y + deltaY,
        element.x, element.y + deltaY, element.w, height, element.texW, element.texH, -1);
    }
  }

  /** Renders the translucent hover overlay used by tank and fuel modules. */
  public static void renderHighlight(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    if (width > 0 && height > 0) {
      graphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
    }
  }

  /** Renders a part-builder pattern sprite. */
  public static void renderPattern(GuiGraphicsExtractor graphics, Pattern pattern, int x, int y) {
    TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
      .get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, pattern.getTexture()));
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, -1);
  }
}

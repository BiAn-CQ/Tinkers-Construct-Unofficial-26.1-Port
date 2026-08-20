package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;

/** Helper for submitting the layered fluid volume inside a heating structure. */
public final class SmelteryTankRenderer {
  private static final float FLUID_OFFSET = 0.005f;
  private static final int HEIGHT_OFFSET = (int)(FLUID_OFFSET * 2000d);

  private SmelteryTankRenderer() {}

  private static float[] getBlockBounds(int delta) {
    return getBlockBounds(delta, FLUID_OFFSET, delta + 1f - FLUID_OFFSET);
  }

  private static float[] getBlockBounds(int delta, float start, float end) {
    float[] bounds = new float[2 + delta];
    bounds[0] = start;
    int offset = (int)start;
    for (int i = 1; i <= delta; i++) {
      bounds[i] = i + offset;
    }
    bounds[delta + 1] = end;
    return bounds;
  }

  public static void submitFluids(PoseStack matrices, SubmitNodeCollector collector, List<FluidStack> fluids, int capacity,
                                  BlockPos tankMinPos, BlockPos tankMaxPos, int brightness) {
    if (fluids.isEmpty() || capacity <= 0) {
      return;
    }
    int xd = tankMaxPos.getX() - tankMinPos.getX();
    int zd = tankMaxPos.getZ() - tankMinPos.getZ();
    if (xd < 0 || zd < 0) {
      return;
    }
    int yd = 1 + Math.max(0, tankMaxPos.getY() - tankMinPos.getY());
    int[] heights = GuiSmelteryTank.calcLiquidHeights(fluids, capacity, yd * 1000 - HEIGHT_OFFSET, 100);
    float[] xBounds = getBlockBounds(xd);
    float[] zBounds = getBlockBounds(zd);
    collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID,
      (pose, buffer) -> renderFluids(pose, buffer, fluids, brightness, xd, xBounds, zd, zBounds, heights));
  }

  private static void renderFluids(PoseStack.Pose pose, VertexConsumer buffer, List<FluidStack> fluids, int brightness,
                                   int xd, float[] xBounds, int zd, float[] zBounds, int[] heights) {
    float currentY = FLUID_OFFSET;
    for (int i = 0; i < fluids.size(); i++) {
      float height = heights[i] / 1000f;
      renderLargeFluidCuboid(pose, buffer, fluids.get(i), brightness, xd, xBounds, zd, zBounds, currentY, currentY + height);
      currentY += height;
    }
  }

  private static void renderLargeFluidCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidStack fluid, int brightness,
                                             int xd, float[] xBounds, int zd, float[] zBounds, float yMin, float yMax) {
    if (yMin >= yMax || fluid.isEmpty()) {
      return;
    }
    FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    TextureAtlasSprite still = fluidModel.stillMaterial().sprite();
    int color = fluidModel.fluidTintSource() == null ? -1 : fluidModel.fluidTintSource().colorAsStack(fluid);
    FluidType fluidType = fluid.getFluid().getFluidType();
    brightness = FluidRenderer.withBlockLight(brightness, fluidType.getLightLevel(fluid));
    boolean upsideDown = fluidType.isLighterThanAir();

    int yd = (int)(yMax - (int)yMin);
    if (yMax % 1d == 0) {
      yd--;
    }
    float[] yBounds = getBlockBounds(yd, yMin, yMax);
    Matrix4f matrix = pose.pose();
    Vector3f from = new Vector3f();
    Vector3f to = new Vector3f();
    int rotation = upsideDown ? 180 : 0;
    for (int y = 0; y <= yd; y++) {
      for (int z = 0; z <= zd; z++) {
        for (int x = 0; x <= xd; x++) {
          from.set(xBounds[x], yBounds[y], zBounds[z]);
          to.set(xBounds[x + 1], yBounds[y + 1], zBounds[z + 1]);
          if (x == 0) FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.WEST, color, brightness, rotation, false);
          if (x == xd) FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.EAST, color, brightness, rotation, false);
          if (z == 0) FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.NORTH, color, brightness, rotation, false);
          if (z == zd) FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.SOUTH, color, brightness, rotation, false);
          if (y == yd) FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.UP, color, brightness, rotation, false);
          if (y == 0) {
            from.y = from.y() + 0.001f;
            FluidRenderer.putTexturedQuad(buffer, matrix, still, from, to, Direction.DOWN, color, brightness, rotation, false);
          }
        }
      }
    }
  }
}

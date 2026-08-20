package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;

import java.util.EnumMap;
import java.util.Map;

/** Native 26.1 renderer for the six special item-frame variants. */
public class FancyItemFrameRendererCompat extends ItemFrameRenderer<FancyItemFrameEntity> {
  private static final Map<FrameType, StandaloneModelKey<BlockStateModel>> MODELS = new EnumMap<>(FrameType.class);
  private static final Map<FrameType, StandaloneModelKey<BlockStateModel>> MAP_MODELS = new EnumMap<>(FrameType.class);

  static {
    for (FrameType type : FrameType.values()) {
      String name = type == FrameType.REVERSED_GOLD ? FrameType.GOLD.getSerializedName() : type.getSerializedName();
      MODELS.put(type, key("block/frame/" + name));
      MAP_MODELS.put(type, key("block/frame/" + name + "_map"));
    }
  }

  private static StandaloneModelKey<BlockStateModel> key(String path) {
    ModelDebugName debugName = () -> TConstruct.getResource(path).toString();
    return new StandaloneModelKey<>(debugName);
  }

  /** Registers every custom frame model as a 26.1 standalone block-state model. */
  static void registerModels(ModelEvent.RegisterStandalone event) {
    for (FrameType type : FrameType.values()) {
      String name = type == FrameType.REVERSED_GOLD ? FrameType.GOLD.getSerializedName() : type.getSerializedName();
      event.register(MODELS.get(type), SimpleUnbakedStandaloneModel.blockStateModel(TConstruct.getResource("block/frame/" + name)));
      event.register(MAP_MODELS.get(type), SimpleUnbakedStandaloneModel.blockStateModel(TConstruct.getResource("block/frame/" + name + "_map")));
    }
  }

  public FancyItemFrameRendererCompat(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  protected int getBlockLightLevel(FancyItemFrameEntity frame, BlockPos pos) {
    int baseLight = super.getBlockLightLevel(frame, pos);
    return frame.getFrameType() == FrameType.MANYULLYN ? Math.max(7, baseLight) : baseLight;
  }

  @Override
  public ItemFrameRenderState createRenderState() {
    return new FancyItemFrameRenderState();
  }

  @Override
  public void extractRenderState(FancyItemFrameEntity entity, ItemFrameRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    if (!(state instanceof FancyItemFrameRenderState fancyState)) {
      return;
    }
    fancyState.frameType = entity.getFrameType();
    boolean hideClearFrame = fancyState.frameType == FrameType.CLEAR && !entity.getItem().isEmpty();
    if (state.isInvisible || hideClearFrame) {
      state.frameModel.clear();
      return;
    }
    StandaloneModelKey<BlockStateModel> key = (state.mapId == null ? MODELS : MAP_MODELS).get(fancyState.frameType);
    BlockStateModel model = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    state.frameModel.clear();
    if (model != null) {
      model.collectParts(RandomSource.create(42), state.frameModel.setupModel(new Matrix4f(), false));
    }
  }

  @Override
  public void submit(ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    if (!(state instanceof FancyItemFrameRenderState fancyState)) {
      super.submit(state, poseStack, submitNodeCollector, camera);
      return;
    }
    submitNameDisplay(state, poseStack, submitNodeCollector, camera);
    poseStack.pushPose();
    Direction direction = state.direction;
    Vec3 renderOffset = getRenderOffset(state);
    poseStack.translate(-renderOffset.x(), -renderOffset.y(), -renderOffset.z());
    poseStack.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
    float xRot = direction.getAxis().isHorizontal() ? 0 : -90 * direction.getAxisDirection().getStep();
    float yRot = direction.getAxis().isHorizontal() ? 180 - direction.toYRot() : 180;
    poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

    if (!state.frameModel.isEmpty()) {
      poseStack.pushPose();
      poseStack.translate(-0.5, -0.5, -0.5);
      state.frameModel.submitWithZOffset(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
    }

    poseStack.translate(0, 0, state.isInvisible ? 0.5 : 0.4375);
    if (!NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderItemInFrameEvent(
      state, this, poseStack, submitNodeCollector)).isCanceled()) {
      boolean fullBright = fancyState.frameType == FrameType.MANYULLYN;
      if (state.mapId != null) {
        int rotation = fancyState.frameType.hasMoreRotations() ? state.rotation % 4 * 4 : state.rotation % 4 * 2;
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0f / (fancyState.frameType.hasMoreRotations() ? 16 : 8)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.scale(0.0078125f, 0.0078125f, 0.0078125f);
        poseStack.translate(-64, -64, -1);
        int light = fullBright ? 15728850 : state.lightCoords;
        Minecraft.getInstance().getMapRenderer().render(state.mapRenderState, poseStack, submitNodeCollector, true, light);
      } else if (!state.item.isEmpty()) {
        float divisor = fancyState.frameType.hasMoreRotations() ? 16 : 8;
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation * 360.0f / divisor));
        float scale = fancyState.frameType == FrameType.CLEAR ? 0.75f : 0.5f;
        poseStack.scale(scale, scale, scale);
        state.item.submit(poseStack, submitNodeCollector, fullBright ? 15728880 : state.lightCoords,
          OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
    }
    poseStack.popPose();
  }

  private static final class FancyItemFrameRenderState extends ItemFrameRenderState {
    private FrameType frameType = FrameType.GOLD;
  }
}

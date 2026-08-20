package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.utils.SimpleCache;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;

import java.util.Optional;

/** Native 26.1 combat fishing-hook renderer with material textures and tinting. */
public class CombatFishingHookRendererCompat extends EntityRenderer<CombatFishingHook, FishingHookRenderState> {
  private static final Identifier BASE = texture("");
  private static final SimpleCache<MaterialVariantId, MaterialTexture> TEXTURE_CACHE = new SimpleCache<>(material -> {
    if (IMaterial.UNKNOWN_ID.equals(material)) {
      return new MaterialTexture(BASE, -1, 0);
    }
    Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (infoOptional.isPresent()) {
      MaterialRenderInfo info = infoOptional.get();
      Identifier untinted = info.texture();
      if (untinted != null) {
        Identifier texture = existingTexture("_" + untinted.getNamespace() + "_" + untinted.getPath());
        if (texture != null) {
          return new MaterialTexture(texture, -1, info.luminosity());
        }
      }
      for (String fallback : info.fallbacks()) {
        Identifier texture = existingTexture("_" + fallback);
        if (texture != null) {
          return new MaterialTexture(texture, info.vertexColor(), info.luminosity());
        }
      }
      return new MaterialTexture(BASE, info.vertexColor(), info.luminosity());
    }
    return new MaterialTexture(BASE, -1, 0);
  });

  public CombatFishingHookRendererCompat(EntityRendererProvider.Context context) {
    super(context);
  }

  static void clearCache() {
    TEXTURE_CACHE.clear();
  }

  private static Identifier texture(String suffix) {
    return TConstruct.getResource("textures/tinker_armor/fishing_hook/material" + suffix + ".png");
  }

  private static Identifier existingTexture(String suffix) {
    Identifier texture = texture(suffix);
    return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent() ? texture : null;
  }

  @Override
  public boolean shouldRender(CombatFishingHook entity, Frustum culler, double camX, double camY, double camZ) {
    return super.shouldRender(entity, culler, camX, camY, camZ) && entity.getPlayerOwner() != null;
  }

  @Override
  protected boolean affectedByCulling(CombatFishingHook entity) {
    return false;
  }

  @Override
  public FishingHookRenderState createRenderState() {
    return new CombatFishingHookRenderState();
  }

  @Override
  public void extractRenderState(CombatFishingHook entity, FishingHookRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    Player owner = entity.getPlayerOwner();
    if (owner == null) {
      state.lineOriginOffset = Vec3.ZERO;
    } else {
      float swing = Mth.sin(Mth.sqrt(owner.getAttackAnim(partialTicks)) * (float)Math.PI);
      Vec3 handPos = getPlayerHandPos(owner, swing, partialTicks);
      state.lineOriginOffset = handPos.subtract(entity.getPosition(partialTicks).add(0, 0.25, 0));
    }
    if (state instanceof CombatFishingHookRenderState combatState) {
      combatState.material = TEXTURE_CACHE.apply(entity.getMaterial());
    }
  }

  private Vec3 getPlayerHandPos(Player owner, float swing, float partialTicks) {
    int side = FishingHookRenderer.getHoldingArm(owner) == HumanoidArm.RIGHT ? 1 : -1;
    if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().player) {
      float fov = entityRenderDispatcher.options.fov().get();
      return owner.getEyePosition(partialTicks).add(entityRenderDispatcher.camera.getNearPlane(fov)
        .getPointOnPlane(side * 0.525f, -0.1f)
        .scale(960.0 / fov)
        .yRot(swing * 0.5f)
        .xRot(-swing * 0.7f));
    }
    float rotation = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * (float)(Math.PI / 180.0);
    double sin = Mth.sin(rotation);
    double cos = Mth.cos(rotation);
    float scale = owner.getScale();
    return owner.getEyePosition(partialTicks).add(
      -cos * side * 0.35 * scale - sin * 0.8 * scale,
      (owner.isCrouching() ? -0.1875 : 0) - 0.45 * scale,
      -sin * side * 0.35 * scale + cos * 0.8 * scale);
  }

  @Override
  public void submit(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    MaterialTexture material = state instanceof CombatFishingHookRenderState combatState
      ? combatState.material : new MaterialTexture(BASE, -1, 0);
    poseStack.pushPose();
    poseStack.pushPose();
    poseStack.scale(0.5f, 0.5f, 0.5f);
    poseStack.mulPose(camera.orientation);
    int bobberLight = applyLuminosity(state.lightCoords, material.luminosity);
    collector.submitCustomGeometry(poseStack, material.renderType, (pose, buffer) -> {
      vertex(buffer, pose, bobberLight, material.color, 0, 0, 0, 1);
      vertex(buffer, pose, bobberLight, material.color, 1, 0, 1, 1);
      vertex(buffer, pose, bobberLight, material.color, 1, 1, 1, 0);
      vertex(buffer, pose, bobberLight, material.color, 0, 1, 0, 0);
    });
    poseStack.popPose();

    float x = (float)state.lineOriginOffset.x;
    float y = (float)state.lineOriginOffset.y;
    float z = (float)state.lineOriginOffset.z;
    float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
    collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
      for (int index = 0; index < 16; index++) {
        float start = index / 16.0f;
        float end = (index + 1) / 16.0f;
        stringVertex(x, y, z, buffer, pose, start, end, width);
        stringVertex(x, y, z, buffer, pose, end, start, width);
      }
    });
    poseStack.popPose();
    super.submit(state, poseStack, collector, camera);
  }

  private static int applyLuminosity(int packedLight, int luminosity) {
    if (luminosity <= 0) return packedLight;
    if (luminosity >= 15) return (15 << 4) | (15 << 20);
    int block = Math.max(luminosity, (packedLight & 0xffff) >> 4) << 4;
    int sky = Math.max(luminosity, packedLight >> 20 & 0xffff) << 20;
    return block | sky;
  }

  private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, int color,
                             float x, int y, int u, int v) {
    buffer.addVertex(pose, x - 0.5f, y - 0.5f, 0)
      .setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
      .setLight(light).setNormal(pose, 0, 1, 0);
  }

  private static void stringVertex(float xOffset, float yOffset, float zOffset, VertexConsumer buffer,
                                   PoseStack.Pose pose, float fraction, float nextFraction, float width) {
    float x = xOffset * fraction;
    float y = yOffset * (fraction * fraction + fraction) * 0.5f + 0.25f;
    float z = zOffset * fraction;
    float normalX = xOffset * nextFraction - x;
    float normalY = yOffset * (nextFraction * nextFraction + nextFraction) * 0.5f + 0.25f - y;
    float normalZ = zOffset * nextFraction - z;
    float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
    buffer.addVertex(pose, x, y, z).setColor(0xff000000)
      .setNormal(pose, normalX / length, normalY / length, normalZ / length).setLineWidth(width);
  }

  private static final class CombatFishingHookRenderState extends FishingHookRenderState {
    private MaterialTexture material = new MaterialTexture(BASE, -1, 0);
  }

  private record MaterialTexture(RenderType renderType, int color, int luminosity) {
    private MaterialTexture(Identifier texture, int color, int luminosity) {
      this(RenderTypes.entityCutoutCull(texture), color, luminosity);
    }
  }
}

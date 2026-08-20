package slimeknights.tconstruct.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Slime;
import slimeknights.tconstruct.world.entity.ArmoredSlimeEntity;

/** Native 26.1 slime renderer retaining Tinkers' textures and metal variants. */
public class TinkerSlimeRendererCompat extends SlimeRenderer {
  private final Identifier slimeTexture;
  private final Identifier metalTexture;

  public TinkerSlimeRendererCompat(EntityRendererProvider.Context context, Identifier slimeTexture, Identifier metalTexture) {
    super(context);
    this.slimeTexture = slimeTexture;
    this.metalTexture = metalTexture;
    // Minecraft 26.1's SlimeOuterLayer hardcodes the vanilla green slime
    // texture instead of asking its parent renderer. Replace that layer for
    // every Tinker slime so both the normal and metal texture selected below
    // are also used by the translucent shell.
    layers.clear();
    addLayer(new TinkerSlimeOuterLayerCompat(this, context.getModelSet(), this::getTextureLocation));
    addLayer(new SlimeHeadLayerCompat<>(this, context, false));
  }

  @Override
  public SlimeRenderState createRenderState() {
    return new SlimeEquipmentRenderState();
  }

  @Override
  public void extractRenderState(Slime entity, SlimeRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    if (state instanceof SlimeEquipmentRenderState tinkerState) {
      tinkerState.metal = entity instanceof ArmoredSlimeEntity armored && armored.isMetal();
      HumanoidMobRenderer.extractHumanoidRenderState(entity, tinkerState.armorState, partialTicks, itemModelResolver);
    }
  }

  @Override
  public Identifier getTextureLocation(SlimeRenderState state) {
    return state instanceof SlimeEquipmentRenderState tinkerState && tinkerState.metal ? metalTexture : slimeTexture;
  }
}

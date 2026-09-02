package slimeknights.tconstruct.tools.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CrystalshotRenderer extends ArrowRenderer<CrystalshotEntity, CrystalshotRenderer.State> {
  private static final Map<String,Identifier> TEXTURES = new HashMap<>();
  private static final Function<String,Identifier> TEXTURE_GETTER = variant -> TConstruct.getResource("textures/entity/arrow/" + variant + ".png");
  public CrystalshotRenderer(Context context) {
    super(context);
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(CrystalshotEntity entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.variant = entity.getVariant();
  }

  @Override
  protected Identifier getTextureLocation(State state) {
    return TEXTURES.computeIfAbsent(state.variant, TEXTURE_GETTER);
  }

  /** Render-state copy of the projectile's entity-only variant. */
  public static final class State extends ArrowRenderState {
    private String variant = "default";
  }
}

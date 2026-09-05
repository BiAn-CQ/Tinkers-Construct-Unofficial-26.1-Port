package slimeknights.tconstruct.client;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity.StandType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Native 26.1 renderer for the fancy armor stand variants. */
public class FancyArmorStandRendererCompat extends ArmorStandRenderer {
  private static final Map<StandType, Identifier> TEXTURES = new EnumMap<>(StandType.class);

  static {
    Identifier base = TConstruct.getResource("textures/entity/armorstand/");
    for (StandType type : StandType.values()) {
      TEXTURES.put(type, base.withSuffix(type.name().toLowerCase(Locale.ROOT) + ".png"));
    }
  }

  public FancyArmorStandRendererCompat(EntityRendererProvider.Context context) {
    super(context);
  }

  /** Small stands retain adult armor texture layouts, regardless of their entity type. */
  public static boolean usesBabyArmorTextures(HumanoidRenderState state) {
    return state.isBaby && !(state instanceof ArmorStandRenderState);
  }

  @Override
  public ArmorStandRenderState createRenderState() {
    return new FancyArmorStandRenderState();
  }

  @Override
  public void extractRenderState(ArmorStand entity, ArmorStandRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    FancyArmorStandRenderState fancyState = (FancyArmorStandRenderState) state;
    FancyArmorStandEntity fancyEntity = (FancyArmorStandEntity) entity;
    fancyState.standType = fancyEntity.getStandType();
    fancyState.clearHidden = fancyEntity.isClearHidden();
    if (fancyState.standType.isFullbright()) {
      state.lightCoords = 0x00F000F0;
    }
  }

  @Override
  public Identifier getTextureLocation(ArmorStandRenderState state) {
    return TEXTURES.get(((FancyArmorStandRenderState) state).standType);
  }

  @Override
  protected boolean isBodyVisible(ArmorStandRenderState state) {
    return !((FancyArmorStandRenderState) state).clearHidden && super.isBodyVisible(state);
  }

  private static final class FancyArmorStandRenderState extends ArmorStandRenderState {
    private StandType standType = StandType.BAMBOO;
    private boolean clearHidden;
  }
}

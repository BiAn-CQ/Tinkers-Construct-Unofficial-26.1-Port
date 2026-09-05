package slimeknights.tconstruct.client;

import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorStandArmorTextureTest {
  @Test
  void smallStandsUseAdultTexturesWithoutChangingTheirSize() {
    ArmorStandRenderState state = new ArmorStandRenderState();
    state.isBaby = true;
    state.isSmall = true;
    assertThat(FancyArmorStandRendererCompat.usesBabyArmorTextures(state)).isFalse();
    assertThat(state.isBaby).isTrue();
    assertThat(state.isSmall).isTrue();
  }

  @Test
  void ordinaryHumanoidsKeepTheirAgeBasedTextures() {
    HumanoidRenderState state = new HumanoidRenderState();
    assertThat(FancyArmorStandRendererCompat.usesBabyArmorTextures(state)).isFalse();
    state.isBaby = true;
    assertThat(FancyArmorStandRendererCompat.usesBabyArmorTextures(state)).isTrue();
  }
}

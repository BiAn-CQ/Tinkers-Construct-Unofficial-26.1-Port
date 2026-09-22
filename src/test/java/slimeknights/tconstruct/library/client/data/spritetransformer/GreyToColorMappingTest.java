package slimeknights.tconstruct.library.client.data.spritetransformer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreyToColorMappingTest {
  @Test
  void palettesProduceNativeArgbColors() {
    GreyToColorMapping mapping = GreyToColorMapping.builder().addARGB(0, 0xFF000000).addARGB(216, 0xFF14B485).build();
    assertThat(mapping.mapColor(0xFFD8D8D8)).isEqualTo(0xFF14B485);
    assertThat(GreyToColorMapping.builder().addABGR(0, 0xFF000000).addABGR(216, 0xFF85B414).build().mapColor(0xFFD8D8D8)).isEqualTo(0xFF14B485);
    assertThat(GreyToSpriteTransformer.builder().addARGB(0, 0xFF000000).addARGB(216, 0xFF14B485).build().getFallbackColor()).isEqualTo(0xFF14B485);
  }

  @Test
  void interpolationAndTintKeepRedAndBlueChannels() {
    assertThat(GreyToColorMapping.interpolateColors(0xFF102030, 0, 0xFF506070, 100, 50)).isEqualTo(0xFF304050);
    assertThat(GreyToColorMapping.scaleColor(0x80FF8040, 0xFFFF8040, 255)).isEqualTo(0x80FF4010);
  }
}

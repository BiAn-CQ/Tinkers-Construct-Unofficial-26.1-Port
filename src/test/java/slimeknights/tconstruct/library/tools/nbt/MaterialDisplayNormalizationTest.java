package slimeknights.tconstruct.library.tools.nbt;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MaterialDisplayNormalizationTest {
  @Test
  void replacingDisplayMaterialPadsMissingPartsWithoutChangingOriginal() {
    MaterialId iron = new MaterialId("tconstruct", "iron");
    MaterialId wood = new MaterialId("tconstruct", "wood");
    MaterialIdNBT original = new MaterialIdNBT(List.of(iron));
    assertThat(original.replaceMaterial(2, wood).getMaterials()).containsExactly(iron, MaterialId.UNKNOWN, wood);
    assertThat(original.replaceMaterial(0, wood).getMaterials()).containsExactly(wood);
    assertThat(original.getMaterials()).containsExactly(iron);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> original.replaceMaterial(-1, wood))
      .isInstanceOf(IndexOutOfBoundsException.class);
  }
  @Test
  void defaultVariantNormalizesButNamedVariantRemains() {
    MaterialId id = new MaterialId("tconstruct", "iron");
    MaterialVariantId defaultId = MaterialVariantId.create(id, "default");
    MaterialVariantId named = MaterialVariantId.create(id, "polished");
    assertThat(id.isDefaultVariant()).isFalse();
    assertThat(id.normalizeVariant()).isSameAs(id);
    assertThat(defaultId.isDefaultVariant()).isTrue();
    assertThat(defaultId.normalizeVariant()).isEqualTo(id);
    assertThat(named.normalizeVariant()).isSameAs(named);
  }

  @Test
  void displayedMaterialsKeepPrefixAndAppendRequiredPartsWithoutMutatingInput() {
    MaterialId iron = new MaterialId("tconstruct", "iron");
    MaterialId wood = new MaterialId("tconstruct", "wood");
    MaterialId stone = new MaterialId("tconstruct", "stone");
    MaterialIdNBT original = new MaterialIdNBT(List.of(iron, wood));
    assertThat(original.normalize(1, List.of(stone)).getMaterials()).containsExactly(iron, stone);
    assertThat(original.getMaterials()).containsExactly(iron, wood);
    assertThat(original.size()).isEqualTo(2);
    assertThat(original.normalize(2, List.of())).isSameAs(original);
  }
}

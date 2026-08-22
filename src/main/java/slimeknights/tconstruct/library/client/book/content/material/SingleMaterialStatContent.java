package slimeknights.tconstruct.library.client.book.content.material;

import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.util.html.HtmlSerializable;
import slimeknights.tconstruct.library.client.book.content.AbstractMaterialContent;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;

/** Base page for material categories supporting one stat type. */
public abstract class SingleMaterialStatContent extends AbstractMaterialContent {
  protected SingleMaterialStatContent(MaterialVariantId materialVariant, boolean detailed) {
    super(materialVariant, detailed);
  }

  protected abstract MaterialStatsId getStatType();

  protected abstract boolean hasPart();

  protected abstract String translationSuffix();

  @Override
  protected String getTextKey(MaterialId material) {
    String root = "material." + material.getNamespace() + '.' + material.getPath() + (detailed ? ".encyclopedia" : ".flavor");
    String specialized = root + '.' + translationSuffix();
    return Util.canTranslate(specialized) ? specialized : root;
  }

  @Nullable
  @Override
  protected MaterialStatsId getStatType(int index) {
    return index == 0 ? getStatType() : null;
  }

  @Override
  protected int getStatRows() {
    return 1;
  }

  @Override
  protected boolean supportsStatType(MaterialStatsId statsId) {
    return statsId.equals(getStatType());
  }

  @Override
  protected HtmlSerializable makeStatsHtml(BookData data) {
    return makeStatHtml(getStatType(), true, hasPart());
  }
}

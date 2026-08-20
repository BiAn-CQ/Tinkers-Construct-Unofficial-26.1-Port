package slimeknights.tconstruct.library.recipe.material;

import lombok.NoArgsConstructor;
import net.minecraft.data.recipes.RecipeOutput;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.ArrayList;
import java.util.List;

/** @deprecated use {@link MaterialsConsumerBuilder}. */
@Deprecated
@NoArgsConstructor(staticName = "wrap")
public class ShapedMaterialConsumerBuilder {
  private final List<MaterialVariantId> materials = new ArrayList<>();

  public ShapedMaterialConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  public RecipeOutput build(RecipeOutput output) {
    MaterialsConsumerBuilder builder = MaterialsConsumerBuilder.shaped("#");
    materials.forEach(builder::material);
    return builder.build(output);
  }
}

package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.RandomSource;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fills missing material slots while remapping legacy material IDs at matching indexes.
 * Remapping runs only when the missing-material hook expands an older material list.
 */
public record RemappingMaterialsModule(List<RandomMaterial> materials,
                                       List<Map<MaterialId,MaterialVariantId>> remap)
  implements MissingMaterialsToolHook, ToolModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.defaultHooks(ToolHooks.MISSING_MATERIALS);
  public static final RecordLoadable<RemappingMaterialsModule> LOADER = RecordLoadable.create(
    RandomMaterial.LOADER.list(1).requiredField("missing", RemappingMaterialsModule::materials),
    MaterialId.PARSER.mapWithValues(MaterialVariantId.LOADABLE, 0).list(1)
      .requiredField("remap", RemappingMaterialsModule::remap),
    RemappingMaterialsModule::new);

  @Override
  public RecordLoadable<RemappingMaterialsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public MaterialNBT fillMaterials(ToolDefinition definition, RandomSource random) {
    return RandomMaterial.build(ToolMaterialHook.stats(definition), materials, random);
  }

  @Override
  public MaterialNBT fillMaterials(ToolDefinition definition, MaterialNBT existing, RandomSource random) {
    MaterialNBT defaults = fillMaterials(definition, random);
    int oldSize = existing.size();
    if (oldSize == 0) {
      return defaults;
    }
    int newSize = defaults.size();
    List<MaterialVariant> resolved = new ArrayList<>(newSize);
    for (int index = 0; index < oldSize; index++) {
      MaterialVariant material = existing.get(index);
      if (index < remap.size()) {
        MaterialVariantId replacement = remap.get(index).get(material.getId());
        if (replacement != null) {
          material = MaterialVariant.of(replacement);
        }
      }
      resolved.add(material);
    }
    for (int index = oldSize; index < newSize; index++) {
      resolved.add(defaults.get(index));
    }
    return new MaterialNBT(resolved);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final ImmutableList.Builder<RandomMaterial> materials = ImmutableList.builder();
    private final ImmutableList.Builder<Map<MaterialId,MaterialVariantId>> remap = ImmutableList.builder();

    private Builder() {}

    public Builder material(RandomMaterial material) {
      materials.add(material);
      return this;
    }

    public Builder material(RandomMaterial... materials) {
      for (RandomMaterial material : materials) {
        material(material);
      }
      return this;
    }

    public Builder material(MaterialVariantId material) {
      return material(RandomMaterial.fixed(material));
    }

    public Remap remap() {
      return new Remap();
    }

    public RemappingMaterialsModule build() {
      List<RandomMaterial> materials = this.materials.build();
      List<Map<MaterialId,MaterialVariantId>> remap = this.remap.build();
      if (materials.isEmpty()) {
        throw new IllegalArgumentException("Must have at least 1 material");
      }
      if (remap.isEmpty()) {
        throw new IllegalArgumentException("Must have at least 1 remap. For 0, use DefaultMaterialsModule");
      }
      return new RemappingMaterialsModule(materials, remap);
    }

    public class Remap {
      private final ImmutableMap.Builder<MaterialId,MaterialVariantId> remap = ImmutableMap.builder();

      public Remap add(MaterialId material, MaterialVariantId replacement) {
        remap.put(material, replacement);
        return this;
      }

      public Builder end() {
        Builder.this.remap.add(remap.build());
        return Builder.this;
      }
    }
  }
}

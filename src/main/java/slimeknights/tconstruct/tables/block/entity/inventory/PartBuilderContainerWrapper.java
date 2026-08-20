package slimeknights.tconstruct.tables.block.entity.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.material.MaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderContainer;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;

import javax.annotation.Nullable;
import java.util.Objects;

public class PartBuilderContainerWrapper implements IPartBuilderContainer {
  private final PartBuilderBlockEntity builder;
  /** If true, the material recipe is out of date*/
  private boolean materialNeedsUpdate = true;
  /** Cached material recipe, may be null if not a material item */
  @Nullable
  private IMaterialValue material = null;
  /** True when the client received the material value from the server. */
  private boolean materialSynced = false;

  public PartBuilderContainerWrapper(PartBuilderBlockEntity builder) {
    this.builder = builder;
  }

  @Override
  public ItemStack getStack() {
    return builder.getItem(PartBuilderBlockEntity.MATERIAL_SLOT);
  }

  @Override
  public ItemStack getPatternStack() {
    return builder.getItem(PartBuilderBlockEntity.PATTERN_SLOT);
  }

  /** Gets the tiles world */
  protected Level getWorld() {
    return Objects.requireNonNull(builder.getLevel(), "Tile entity world must be nonnull");
  }

  /** Refreshes the stored material */
  public void refreshMaterial() {
    this.materialNeedsUpdate = true;
    this.material = null;
    this.materialSynced = false;
  }

  /** Updates the client-side material display without requiring a client recipe manager. */
  public void setSyncedMaterial(@Nullable MaterialVariantId materialId, int value, int needed, ItemStack leftover) {
    this.material = materialId == null ? null : new SyncedMaterialValue(materialId, value, needed, leftover);
    this.materialNeedsUpdate = false;
    this.materialSynced = true;
  }

  @Override
  @Nullable
  public IMaterialValue getMaterial() {
    if (this.materialSynced) {
      return this.material;
    }
    if (this.materialNeedsUpdate) {
      this.materialNeedsUpdate = false;
      ItemStack stack = getStack();
      if (stack.isEmpty()) {
        this.material = null;
      } else if (stack.is(TinkerTags.Items.TOOL_PARTS)) {
        MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
        int cost = MaterialCastingLookup.getItemCost(stack.getItem());
        if (cost == 0 || IMaterial.UNKNOWN_ID.matchesVariant(material)) {
          this.material = null;
        } else {
          this.material = new MaterialValue(material, cost);
        }
      } else {
        Level world = getWorld();
        var manager = slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getRecipeManager(world);
        this.material = manager.getRecipeFor(TinkerRecipeTypes.MATERIAL.get(), this, world)
          .map(holder -> holder.value()).orElse(null);
      }
    }
    return this.material;
  }

  /** Minimal client-side material value reconstructed from the server display packet. */
  private static final class SyncedMaterialValue implements IMaterialValue {
    private final MaterialVariant material;
    private final int value;
    private final int needed;
    private final ItemStack leftover;

    private SyncedMaterialValue(MaterialVariantId material, int value, int needed, ItemStack leftover) {
      this.material = MaterialVariant.of(material);
      this.value = value;
      this.needed = needed;
      this.leftover = leftover.copy();
    }

    @Override
    public MaterialVariant getMaterial() {
      return material;
    }

    @Override
    public int getValue() {
      return value;
    }

    @Override
    public int getNeeded() {
      return needed;
    }

    @Override
    public boolean hasLeftover() {
      return !leftover.isEmpty();
    }

    @Override
    public ItemStack getLeftover() {
      return leftover.copy();
    }
  }
}

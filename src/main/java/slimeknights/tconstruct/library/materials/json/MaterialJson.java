package slimeknights.tconstruct.library.materials.json;

import lombok.Data;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.library.json.JsonRedirect;

import javax.annotation.Nullable;

@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
@Internal
public class MaterialJson {
  @Nullable
  private final ICondition condition;
  @Nullable
  private final Boolean craftable;
  @Nullable
  private final Integer tier;
  @Nullable
  private final Integer sortOrder;
  @Nullable
  private final Boolean hidden;
  @Nullable
  private final JsonRedirect[] redirect;

  public MaterialJson(@Nullable ICondition condition, @Nullable Boolean craftable, @Nullable Integer tier, @Nullable Integer sortOrder, @Nullable Boolean hidden, @Nullable JsonRedirect[] redirect) {
    this.condition = condition;
    this.craftable = craftable;
    this.tier = tier;
    this.sortOrder = sortOrder;
    this.hidden = hidden;
    this.redirect = redirect;
  }

  public ICondition getCondition() { return condition; }
  public Boolean getCraftable() { return craftable; }
  public Integer getTier() { return tier; }
  public Integer getSortOrder() { return sortOrder; }
  public Boolean getHidden() { return hidden; }
  public JsonRedirect[] getRedirect() { return redirect; }
}

package slimeknights.tconstruct.library.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import slimeknights.mantle.data.loadable.common.CompoundTagLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Predicate for matching the native custom-data component, with network serialization support. */
public record CustomDataPredicate(@Nullable CompoundTag tag) implements Predicate<CompoundTag> {
  /** Loadable instance */
  public static final RecordLoadable<CustomDataPredicate> LOADABLE = CompoundTagLoadable.INSTANCE.flatXmap(CustomDataPredicate::new, p -> p.tag);
  /** Instance that matches any custom data. */
  public static final CustomDataPredicate ANY = new CustomDataPredicate(null);

  @Override
  public boolean test(@Nullable CompoundTag toTest) {
    return NbtUtils.compareNbt(this.tag, toTest, true);
  }
}

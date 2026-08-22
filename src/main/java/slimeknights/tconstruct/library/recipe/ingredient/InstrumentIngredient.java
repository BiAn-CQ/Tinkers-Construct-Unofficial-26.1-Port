package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching an instrument item with one exact instrument, or excluding a tag of known variants. */
public final class InstrumentIngredient implements ICustomIngredient {
  public static final LoadableIngredientSerializer<InstrumentIngredient> SERIALIZER = new LoadableIngredientSerializer<>(RecordLoadable.create(
    Loadables.ITEM.requiredField("item", ingredient -> ingredient.item),
    Loadables.RESOURCE_LOCATION.nullableField("instrument", ingredient -> ingredient.instrument),
    Loadables.RESOURCE_LOCATION.nullableField("ignore", ingredient -> ingredient.ignore),
    InstrumentIngredient::new));
  public static final IngredientType<InstrumentIngredient> TYPE = new IngredientType<>(SERIALIZER.codec());

  private final Item item;
  @Nullable private final Identifier instrument;
  @Nullable private final Identifier ignore;

  private InstrumentIngredient(Item item, @Nullable Identifier instrument, @Nullable Identifier ignore) {
    if ((instrument == null) == (ignore == null)) {
      throw new IllegalArgumentException("Instrument ingredient requires exactly one of instrument or ignore");
    }
    this.item = item;
    this.instrument = instrument;
    this.ignore = ignore;
  }

  /** Creates an ingredient matching one exact instrument variant. */
  public static Ingredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument.identifier(), null).toVanilla();
  }

  /** Creates a fallback ingredient matching instruments outside the supplied variant tag, including an unset component. */
  public static Ingredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore.location()).toVanilla();
  }

  @Override
  public boolean test(ItemStack stack) {
    if (!stack.is(item)) {
      return false;
    }
    InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
    if (component == null) {
      return instrument == null;
    }
    Holder<Instrument> holder = component.instrument();
    if (instrument != null) {
      return holder.unwrapKey().map(key -> key.identifier().equals(instrument)).orElse(false);
    }
    return !holder.is(TagKey.create(Registries.INSTRUMENT, Objects.requireNonNull(ignore)));
  }

  @Override
  public Stream<Holder<Item>> items() {
    return Stream.of(item.builtInRegistryHolder());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public SlotDisplay display() {
    ItemStack stack = new ItemStack(item);
    return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof InstrumentIngredient other
      && item.equals(other.item) && Objects.equals(instrument, other.instrument) && Objects.equals(ignore, other.ignore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, instrument, ignore);
  }
}

package slimeknights.tconstruct.library.json.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import slimeknights.tconstruct.common.TinkerModule;

/** Matches a Tinkers' fluid container without decoding a FluidStack during datapack loading. */
public record FluidContentPredicate(Fluid fluid, int amount) implements DataComponentPredicate {
  public static final Codec<FluidContentPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(FluidContentPredicate::fluid),
    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidContentPredicate::amount)
  ).apply(instance, FluidContentPredicate::new));
  public static final DataComponentPredicate.Type<FluidContentPredicate> TYPE = new DataComponentPredicate.ConcreteType<>(CODEC);

  @Override
  public boolean matches(DataComponentGetter components) {
    SimpleFluidContent stored = components.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    return stored != null && stored.is(fluid) && stored.getAmount() == amount;
  }
}

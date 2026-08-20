package slimeknights.tconstruct.shared.particle;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Particle data for a fluid particle */
@RequiredArgsConstructor
public class FluidParticleData implements ParticleOptions {

  @Getter
  private final ParticleType<FluidParticleData> type;
  @Getter
  private final FluidStack fluid;

  public ParticleType<FluidParticleData> getType() { return type; }
  public FluidStack getFluid() { return fluid; }

  /** Particle type for a fluid particle */
  public static class Type extends ParticleType<FluidParticleData> {
    public Type() {
      super(false);
    }

    @Override
    public MapCodec<FluidParticleData> codec() {
      return FluidStack.CODEC.fieldOf("fluid").xmap(fluid -> new FluidParticleData(this, fluid), FluidParticleData::getFluid);
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FluidParticleData> streamCodec() {
      return new StreamCodec<RegistryFriendlyByteBuf,FluidParticleData>() {
        @Override
        public FluidParticleData decode(RegistryFriendlyByteBuf buffer) {
          return new FluidParticleData(Type.this, FluidStack.STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FluidParticleData data) {
          FluidStack.STREAM_CODEC.encode(buffer, data.fluid);
        }
      };
    }
  }
}

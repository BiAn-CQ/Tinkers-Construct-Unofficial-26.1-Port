package slimeknights.tconstruct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ItemLike;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;

import javax.annotation.Nullable;

/** 26.1 item-particle implementation for slime and terracube jumps. */
final class TinkerSlimeParticle extends BreakingItemParticle {
  private TinkerSlimeParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, ItemStack stack) {
    super(level, x, y, z, spriteFor(stack));
    setParticleSpeed(xd, yd, zd);
  }

  private static TextureAtlasSprite spriteFor(ItemStack stack) {
    ItemStackRenderState state = new ItemStackRenderState();
    Minecraft.getInstance().getItemModelResolver().updateForTopItem(
      state, stack, ItemDisplayContext.GROUND, null, null, 0);
    Material.Baked material = state.pickParticleMaterial(RandomSource.create());
    return material.sprite();
  }

  static final class Factory implements ParticleProvider<SimpleParticleType> {
    private final ItemLike item;

    Factory(SlimeType type) {
      this(TinkerCommons.slimeball.get(type));
    }

    Factory(ItemLike item) {
      this.item = item;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xd, double yd, double zd, RandomSource random) {
      return new TinkerSlimeParticle(level, x, y, z, xd, yd, zd, new ItemStack(item));
    }
  }
}

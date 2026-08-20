package slimeknights.tconstruct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supplies terrain particles for tables whose visible material is stored in a
 * block entity rather than in the block state.
 *
 * <p>Vanilla 26.1 resolves the normal destroy particle from the block-state
 * model without a position.  That is sufficient for ordinary blocks, but it
 * cannot see Tinkers' material model data.  The NeoForge client extension is
 * the position-aware path, so the particle sprite is taken from the same
 * dynamic model path used for the placed block.</p>
 */
public final class DynamicTableParticleExtensions implements IClientBlockExtensions {
  static final DynamicTableParticleExtensions INSTANCE = new DynamicTableParticleExtensions();
  /**
   * Chunk meshing passes a snapshot region rather than the live ClientLevel.
   * Keep the cache keyed by position so the position-aware model data survives
   * until the destroy event, which is invoked on the live client level.
   */
  private static final Map<BlockPos,ParticleInfo> PARTICLE_CACHE = new ConcurrentHashMap<>();
  /** Level used to discard positions from a previous client world. */
  @Nullable
  private static volatile ClientLevel activeLevel;

  private DynamicTableParticleExtensions() {}

  public static void cacheParticleSprite(BlockAndTintGetter level, BlockPos pos, TextureAtlasSprite sprite) {
    cacheParticleSprite(level, pos, sprite, -1);
  }

  /**
   * Caches the position-aware particle sprite and, when the sprite is a
   * fallback material sprite, the color that the model baked into its faces.
   * TerrainParticle applies the block-state tint before the dynamic sprite is
   * installed, so the material color must be restored explicitly here.
   */
  public static void cacheParticleSprite(BlockAndTintGetter level, BlockPos pos,
                                         TextureAtlasSprite sprite, int color) {
    // This method is deliberately not restricted to ClientLevel: the native
    // block model is normally called with a RenderChunkRegion snapshot.
    if (level instanceof ClientLevel clientLevel) {
      activateLevel(clientLevel);
    }
    BlockPos key = pos.immutable();
    ParticleInfo value = new ParticleInfo(sprite, color);
    PARTICLE_CACHE.put(key, value);
  }

  private static void activateLevel(ClientLevel level) {
    if (activeLevel == null) {
      activeLevel = level;
    } else if (activeLevel != level) {
      synchronized (PARTICLE_CACHE) {
        if (activeLevel != level) {
          PARTICLE_CACHE.clear();
          activeLevel = level;
        }
      }
    }
  }

  /** Clears position data when the client world or its resource atlases changes. */
  static void clearCache() {
    synchronized (PARTICLE_CACHE) {
      PARTICLE_CACHE.clear();
      activeLevel = null;
    }
  }

  /** Registers the extension for every Tinkers table with dynamic appearance. */
  static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
    event.registerBlock(INSTANCE,
      TinkerTables.tinkersAnvil.get(), TinkerTables.scorchedAnvil.get(),
      TinkerTables.craftingStation.get(), TinkerTables.tinkerStation.get(),
      TinkerTables.partBuilder.get(), TinkerTables.modifierWorktable.get());
  }

  @Override
  public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine particles) {
    if (!(level instanceof ClientLevel clientLevel)) {
      return false;
    }
    activateLevel(clientLevel);
    // Match the vanilla extension contract: returning true means that this
    // extension handled the event, including blocks that intentionally do not
    // spawn terrain particles.
    if (!state.shouldSpawnTerrainParticles()) {
      return true;
    }

    ParticleInfo cached = PARTICLE_CACHE.get(pos);
    // Always ask the live level first. Chunk meshing may have cached the
    // fallback texture before the block entity's model data reached its
    // snapshot; at destroy time the live block entity is still available.
    TextureAtlasSprite modelSprite = particleSprite(clientLevel, pos, state);
    ParticleInfo resolved = PARTICLE_CACHE.get(pos);
    // MaterialBlockBaked records the exact dynamic sprite separately because
    // the baked part's static particle material may still be the gray fallback.
    TextureAtlasSprite sprite = resolved == null ? modelSprite : resolved.sprite();
    if (sprite == null && cached != null) {
      sprite = cached.sprite();
      resolved = cached;
    }
    if (sprite == null) {
      return false;
    }
    int color = resolved == null ? -1 : resolved.color();
    TextureAtlasSprite particleSprite = sprite;

    VoxelShape shape = state.getShape(clientLevel, pos);
    RandomSource random = clientLevel.getRandom();
    shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
      double sizeX = Math.min(1.0D, maxX - minX);
      double sizeY = Math.min(1.0D, maxY - minY);
      double sizeZ = Math.min(1.0D, maxZ - minZ);
      int countX = Math.max(2, Mth.ceil(sizeX / 0.25D));
      int countY = Math.max(2, Mth.ceil(sizeY / 0.25D));
      int countZ = Math.max(2, Mth.ceil(sizeZ / 0.25D));

      for (int x = 0; x < countX; x++) {
        for (int y = 0; y < countY; y++) {
          for (int z = 0; z < countZ; z++) {
            double particleX = pos.getX() + minX + (x + random.nextDouble()) * sizeX / countX;
            double particleY = pos.getY() + minY + (y + random.nextDouble()) * sizeY / countY;
            double particleZ = pos.getZ() + minZ + (z + random.nextDouble()) * sizeZ / countZ;
            particles.add(new DynamicTerrainParticle(clientLevel, particleX, particleY, particleZ,
              state, pos, particleSprite, color));
          }
        }
      }
    });
    return true;
  }

  @Nullable
  private static TextureAtlasSprite particleSprite(ClientLevel level, BlockPos pos, BlockState state) {
    BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
    BlockStateModel model = models.get(state);
    return model.particleMaterial(level, pos, state).sprite();
  }

  private record ParticleInfo(TextureAtlasSprite sprite, int color) {}

  /** TerrainParticle uses the static block-state sprite in its constructor. */
  private static final class DynamicTerrainParticle extends TerrainParticle {
    private DynamicTerrainParticle(ClientLevel level, double x, double y, double z,
                                   BlockState state, BlockPos pos, TextureAtlasSprite sprite, int color) {
      super(level, x, y, z, 0.0D, 0.0D, 0.0D, state, pos);
      setSprite(sprite);
      if (color != -1) {
        setColor(
          ((color >> 16) & 0xFF) / 255.0F,
          ((color >> 8) & 0xFF) / 255.0F,
          (color & 0xFF) / 255.0F
        );
      }
    }
  }
}

package slimeknights.tconstruct.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Native client bridge for the small world-visual slice previously owned by
 * {@code WorldClientEvents}.  The old class also contains the pre-26.1 skull
 * and renderer registrations; those are handled by
 * {@link TConstructClientRenderCompat}, so only foliage colors and particles
 * live here.
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructWorldClientCompat {
  private static final Map<FoliageType, int[]> COLOR_MAP = new EnumMap<>(FoliageType.class);

  static {
    for (FoliageType type : FoliageType.values()) {
      COLOR_MAP.put(type, new int[65536]);
    }
  }

  private TConstructWorldClientCompat() {}

  @SubscribeEvent
  static void registerReloadListeners(AddClientReloadListenersEvent event) {
    for (FoliageType type : FoliageType.values()) {
      event.addListener(TConstruct.getResource("slime_colors/" + type.getSerializedName()), new SlimeColorListener(type));
    }
  }

  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    event.registerSpecial(TinkerWorld.skySlimeParticle.get(), new TinkerSlimeParticle.Factory(SlimeType.SKY));
    event.registerSpecial(TinkerWorld.enderSlimeParticle.get(), new TinkerSlimeParticle.Factory(SlimeType.ENDER));
    event.registerSpecial(TinkerWorld.terracubeParticle.get(), new TinkerSlimeParticle.Factory(Items.CLAY_BALL));
  }

  @SubscribeEvent
  static void registerBlockColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
    for (FoliageType type : FoliageType.values()) {
      event.register(
        List.of(new SlimeTintSource(type, null)),
        TinkerWorld.vanillaSlimeGrass.get(type), TinkerWorld.earthSlimeGrass.get(type),
        TinkerWorld.skySlimeGrass.get(type), TinkerWorld.enderSlimeGrass.get(type),
        TinkerWorld.ichorSlimeGrass.get(type));
      event.register(
        List.of(new SlimeTintSource(type, LOOP_OFFSET)),
        TinkerWorld.slimeLeaves.get(type));
      event.register(
        List.of(new SlimeTintSource(type, null)),
        TinkerWorld.slimeFern.get(type), TinkerWorld.slimeTallGrass.get(type),
        TinkerWorld.pottedSlimeFern.get(type));
    }

    event.register(
      List.of(new SlimeTintSource(FoliageType.SKY, LOOP_OFFSET)),
      TinkerWorld.skySlimeVine.get());
    event.register(
      List.of(new SlimeTintSource(FoliageType.ENDER, LOOP_OFFSET)),
      TinkerWorld.enderSlimeVine.get());
  }

  private static final BlockPos LOOP_OFFSET = BlockPos.containing(128, 0, 128);

  private record SlimeTintSource(FoliageType type, BlockPos offset) implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return type.getColor();
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      return getColorForPos(pos, type, offset);
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      return getColorForPos(pos, type, offset);
    }
  }

  private static int getColorForPos(BlockPos pos, FoliageType type, BlockPos offset) {
    if (pos == null) {
      return type.getColor();
    }
    if (offset != null) {
      pos = pos.offset(offset);
    }
    return getColor(COLOR_MAP.get(type), pos.getX(), pos.getZ());
  }

  private static int getColor(int[] buffer, int posX, int posZ) {
    float x = Math.abs((256 - (Math.abs(posX) % 512)) / 256f);
    float z = Math.abs((256 - (Math.abs(posZ) % 512)) / 256f);
    if (x < z) {
      float swap = x;
      x = z;
      z = swap;
    }
    return buffer[(int) (x * 255f) << 8 | (int) (z * 255f)];
  }

  private static final class SlimeColorListener extends SimplePreparableReloadListener<int[]> {
    private final FoliageType type;
    private final Identifier path;

    private SlimeColorListener(FoliageType type) {
      this.type = type;
      this.path = TConstruct.getResource("textures/colormap/" + type.getSerializedName() + "_grass_color.png");
    }

    @Override
    protected int[] prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
      try {
        return LegacyStuffWrapper.getPixels(resourceManager, path);
      } catch (IOException exception) {
        TConstruct.LOG.error("Failed to load slime colors from {}", path, exception);
        return new int[0];
      }
    }

    @Override
    protected void apply(int[] colors, ResourceManager resourceManager, ProfilerFiller profiler) {
      if (colors.length == 65536) {
        COLOR_MAP.put(type, colors);
      }
    }
  }
}

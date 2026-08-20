package slimeknights.tconstruct.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.model.data.ModelData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.mantle.client.model.NativeBlockColorData;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Native 26.1 block-state model replacements for Tinkers' dynamic block
 * loaders. The legacy loaders are still used as a resource compatibility
 * boundary, but all state-dependent geometry is baked through the 26.1
 * {@link BlockStateModel} API.
 */
public final class NativeTinkerBlockStateModel {
  /**
   * Returns true when the current client model set resolved the state to the
   * native tank model. Legacy block entity renderers use this as a fallback
   * guard, avoiding a second fluid pass while still working with resource
   * packs or older blockstate definitions that do not resolve natively.
   */
  public static boolean isNativeTankModel(BlockState state) {
    if (Minecraft.getInstance().getModelManager() == null) {
      return false;
    }
    BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
    // When the compatibility setting is disabled, TankBaked intentionally
    // returns only the shell and the block-entity renderer supplies the
    // liquid.  Treat that mode as non-native here, otherwise the legacy
    // renderer would skip its pass and tanks would glow without a fluid.
    return models.get(state) instanceof TankBaked tank && tank.rendersFluidInModel();
  }

  private NativeTinkerBlockStateModel() {}

  private static final Codec<FluidFace> FLUID_FACE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Codec.BOOL.optionalFieldOf("flowing", false).forGetter(FluidFace::flowing),
    Codec.INT.optionalFieldOf("rotation", 0).forGetter(FluidFace::rotation)
  ).apply(instance, FluidFace::new));

  private record FluidFace(boolean flowing, int rotation) {
    private FluidFace {
      if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
        throw new IllegalArgumentException("Fluid face rotation must be 0/90/180/270");
      }
    }
  }

  /** Native replacement for {@code loader: tconstruct:tank}. */
  public record TankUnbaked(
    Identifier model,
    Vector3fc fluidFrom,
    Vector3fc fluidTo,
    int increments,
    Map<String,FluidFace> fluidFaces,
    boolean forceModelFluid,
    Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<TankUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.fieldOf("model").forGetter(TankUnbaked::model),
      ExtraCodecs.VECTOR3F.fieldOf("fluid_from").forGetter(TankUnbaked::fluidFrom),
      ExtraCodecs.VECTOR3F.fieldOf("fluid_to").forGetter(TankUnbaked::fluidTo),
      Codec.intRange(1, Integer.MAX_VALUE).fieldOf("increments").forGetter(TankUnbaked::increments),
      Codec.unboundedMap(Codec.STRING, FLUID_FACE_CODEC).optionalFieldOf("fluid_faces", Map.of()).forGetter(TankUnbaked::fluidFaces),
      Codec.BOOL.optionalFieldOf("force_model_fluid", false).forGetter(TankUnbaked::forceModelFluid),
      Variant.SimpleModelState.MAP_CODEC.forGetter(TankUnbaked::modelState)
    ).apply(instance, TankUnbaked::new));

    public TankUnbaked {
      fluidFaces = Map.copyOf(fluidFaces);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public BlockStateModel bake(ModelBaker modelBaker) {
      return new TankBaked(this, modelBaker);
    }
  }

  /** Native replacement for {@code loader: tconstruct:fluid_texture}. */
  public record FluidTextureUnbaked(
    Identifier model,
    List<String> fluidSlots,
    List<String> retexturedSlots,
    List<NativeBlockColorData> colors,
    Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<FluidTextureUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.fieldOf("model").forGetter(FluidTextureUnbaked::model),
      Codec.STRING.listOf().optionalFieldOf("fluid_slots", List.of()).forGetter(FluidTextureUnbaked::fluidSlots),
      Codec.STRING.listOf().optionalFieldOf("retextured_slots", List.of()).forGetter(FluidTextureUnbaked::retexturedSlots),
      NativeBlockColorData.CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(FluidTextureUnbaked::colors),
      Variant.SimpleModelState.MAP_CODEC.forGetter(FluidTextureUnbaked::modelState)
    ).apply(instance, FluidTextureUnbaked::new));

    public FluidTextureUnbaked {
      fluidSlots = List.copyOf(fluidSlots);
      retexturedSlots = List.copyOf(retexturedSlots);
      colors = List.copyOf(colors);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public BlockStateModel bake(ModelBaker modelBaker) {
      return new FluidTextureBaked(this, modelBaker);
    }
  }

  /** Native replacement for {@code loader: tconstruct:material_block}. */
  public record MaterialBlockUnbaked(
    Identifier model,
    List<String> slots,
    Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<MaterialBlockUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.fieldOf("model").forGetter(MaterialBlockUnbaked::model),
      Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(MaterialBlockUnbaked::slots),
      Variant.SimpleModelState.MAP_CODEC.forGetter(MaterialBlockUnbaked::modelState)
    ).apply(instance, MaterialBlockUnbaked::new));

    public MaterialBlockUnbaked {
      slots = List.copyOf(slots);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public BlockStateModel bake(ModelBaker modelBaker) {
      return new MaterialBlockBaked(this, modelBaker);
    }
  }

  /** Common native block model operations. */
  private abstract static class BaseBaked implements BlockStateModel {
    protected final ModelBaker baker;
    protected final ResolvedModel resolved;
    protected final ModelState modelState;
    protected final BlockStateModelPart fallback;
    @Nullable
    protected final UnbakedCuboidGeometry geometry;
    protected final TextureSlots baseTextures;
    protected final TextureSlots.Data baseTextureData;

    protected BaseBaked(Identifier model, Variant.SimpleModelState state, ModelBaker baker) {
      this.baker = baker;
      this.resolved = baker.getModel(model);
      this.modelState = state.asModelState();
      this.fallback = SimpleModelWrapper.bake(baker, resolved, modelState);
      this.geometry = resolved.getTopGeometry() instanceof UnbakedCuboidGeometry cuboids ? cuboids : null;
      this.baseTextures = resolved.getTopTextureSlots();
      this.baseTextureData = copyBaseTextureData();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
      output.add(fallback);
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
      return fallback.particleMaterial();
    }

    @Override
    public int materialFlags() {
      return fallback.materialFlags();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      output.add(fallback);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return fallback;
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return fallback.particleMaterial();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return fallback.materialFlags();
    }

    /** Bakes a new cuboid part with the given texture overrides. */
    protected BlockStateModelPart bake(List<CuboidModelElement> elements, Map<String,Material> overrides) {
      TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
      overrides.forEach(builder::addTexture);
      TextureSlots textures = new TextureSlots.Resolver()
        // Resolver processes entries in reverse order.  Put the base first so
        // a dynamic slot is allowed to replace the model's original texture.
        .addLast(builder.build())
        .addLast(baseTextureData)
        .resolve(resolved);
      var quads = new UnbakedCuboidGeometry(elements).bake(
        textures, baker, modelState, resolved, resolved.getTopAdditionalProperties()
      );
      return new SimpleModelWrapper(quads, resolved.getTopAmbientOcclusion(), resolved.resolveParticleMaterial(textures, baker));
    }

    private TextureSlots.Data copyBaseTextureData() {
      TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
      if (geometry != null) {
        for (CuboidModelElement element : geometry.elements()) {
          for (CuboidFace face : element.faces().values()) {
            Material material = baseTextures.getMaterial(slot(face.texture()));
            if (material != null) {
              builder.addTexture(slot(face.texture()), material);
            }
          }
        }
      }
      Material particle = baseTextures.getMaterial("particle");
      if (particle != null) {
        builder.addTexture("particle", particle);
      }
      return builder.build();
    }

    /** Returns model data without touching the block entity itself. */
    protected static ModelData modelData(BlockAndTintGetter level, BlockPos pos) {
      return level.getModelData(pos);
    }

    protected static String slot(String texture) {
      return texture.startsWith("#") ? texture.substring(1) : texture;
    }

    protected static CuboidFace copyFace(CuboidFace face, @Nullable ExtraFaceData data) {
      return new CuboidFace(
        face.cullForDirection(), face.tintIndex(), face.texture(), face.uvs(), face.rotation(),
        data == null ? face.faceData() : data, new MutableObject<>()
      );
    }

    protected static List<CuboidModelElement> copyElements(List<CuboidModelElement> source, Function<CuboidFace,CuboidFace> faceMapper) {
      List<CuboidModelElement> result = new ArrayList<>(source.size());
      for (CuboidModelElement element : source) {
        Map<Direction,CuboidFace> faces = new EnumMap<>(Direction.class);
        element.faces().forEach((direction, face) -> faces.put(direction, faceMapper.apply(face)));
        result.add(new CuboidModelElement(
          element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), element.faceData()
        ));
      }
      return result;
    }
  }

  private static final class TankBaked extends BaseBaked {
    private final TankUnbaked definition;
    private final Map<FluidKey,BlockStateModelPart> cache = new ConcurrentHashMap<>();

    private TankBaked(TankUnbaked definition, ModelBaker baker) {
      super(definition.model(), definition.modelState(), baker);
      this.definition = definition;
    }

    private boolean rendersFluidInModel() {
      return definition.forceModelFluid() || Config.CLIENT.tankFluidModel.get();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      FluidKey key = fluidKey(modelData(level, pos));
      if (key == null || (!definition.forceModelFluid() && !Config.CLIENT.tankFluidModel.get())) {
        output.add(fallback);
      } else {
        // A legacy compatibility loader may expose only a baked fallback and
        // not the native cuboid geometry.  Keep that shell and add the fluid
        // part separately instead of silently dropping the liquid.
        if (geometry == null) {
          output.add(fallback);
        }
        output.add(cache.computeIfAbsent(key, this::bakeFluid));
      }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      FluidKey key = fluidKey(modelData(level, pos));
      return key == null || (!definition.forceModelFluid() && !Config.CLIENT.tankFluidModel.get()) ? fallback : key;
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      FluidKey key = fluidKey(modelData(level, pos));
      return key == null || (!definition.forceModelFluid() && !Config.CLIENT.tankFluidModel.get())
             ? fallback.particleMaterial() : cache.computeIfAbsent(key, this::bakeFluid).particleMaterial();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      FluidKey key = fluidKey(modelData(level, pos));
      if (key == null || (!definition.forceModelFluid() && !Config.CLIENT.tankFluidModel.get())) {
        return fallback.materialFlags();
      }
      BlockStateModelPart fluid = cache.computeIfAbsent(key, this::bakeFluid);
      return (geometry == null ? fallback.materialFlags() : 0) | fluid.materialFlags();
    }

    @Nullable
    private FluidKey fluidKey(ModelData data) {
      FluidStack fluid = data.get(ModelProperties.FLUID_STACK);
      if (fluid == null || fluid.isEmpty()) {
        return null;
      }
      Integer capacity = data.get(ModelProperties.TANK_CAPACITY);
      int max = capacity == null || capacity <= 0 ? fluid.getAmount() : capacity;
      int amount = Mth.clamp(fluid.getAmount() * definition.increments() / max, 1, definition.increments());
      return new FluidKey(fluid.copy(), amount);
    }

    private BlockStateModelPart bakeFluid(FluidKey key) {
      FluidStack fluid = key.fluid();
      FluidType type = fluid.getFluid().getFluidType();
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      // Mantle registers a tint source for its data-driven fluids, but keep
      // the FluidTextureManager value as the compatibility fallback for
      // fluids supplied by older datapacks or by another mod.
      int color = fluidModel.fluidTintSource() == null
                  ? FluidTextureManager.getColor(type)
                  : fluidModel.fluidTintSource().colorAsStack(fluid);
      int luminosity = type.getLightLevel(fluid);
      Map<String,Material> textures = Map.of(
        "fluid", new Material(fluidModel.stillMaterial().sprite().contents().name(), fluidModel.stillMaterial().forceTranslucent()),
        "flowing_fluid", new Material(fluidModel.flowingMaterial().sprite().contents().name(), fluidModel.flowingMaterial().forceTranslucent())
      );
      Vector3f from = new Vector3f(definition.fluidFrom());
      Vector3f to = new Vector3f(definition.fluidTo());
      if (type.isLighterThanAir()) {
        from.y = to.y + key.amount() * (from.y - to.y) / definition.increments();
      } else {
        to.y = from.y + key.amount() * (to.y - from.y) / definition.increments();
      }
      ExtraFaceData faceData = new ExtraFaceData(color == -1 ? 0xFFFFFFFF : color, luminosity, true);
      CuboidModelElement fluidElement = fluidElement(from, to, definition.fluidFaces(), faceData);
      List<CuboidModelElement> elements = geometry == null
                                          ? List.of(fluidElement)
                                          : new ArrayList<>(geometry.elements());
      if (geometry != null) {
        elements.add(fluidElement);
      }
      return bake(elements, textures);
    }
  }

  /** Stable tank cache key; the incoming model-data stack is copied frequently. */
  private static final class FluidKey {
    private final FluidStack fluid;
    private final int amount;

    private FluidKey(FluidStack fluid, int amount) {
      this.fluid = fluid;
      this.amount = amount;
    }

    private FluidStack fluid() {
      return fluid;
    }

    private int amount() {
      return amount;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object instanceof FluidKey other) || amount != other.amount) {
        return false;
      }
      return FluidStack.isSameFluidSameComponents(fluid, other.fluid);
    }

    @Override
    public int hashCode() {
      return 31 * amount + FluidStack.hashFluidAndComponents(fluid);
    }
  }

  private static CuboidModelElement fluidElement(Vector3fc from, Vector3fc to, Map<String,FluidFace> configured, ExtraFaceData data) {
    Map<Direction,FluidFace> faces = new EnumMap<>(Direction.class);
    for (Direction direction : Direction.values()) {
      FluidFace face = configured.getOrDefault(direction.getName(), new FluidFace(false, 0));
      faces.put(direction, face);
    }
    Map<Direction,CuboidFace> bakedFaces = new EnumMap<>(Direction.class);
    faces.forEach((direction, face) -> bakedFaces.put(direction, new CuboidFace(
      // This face is constructed after JSON parsing, so use the canonical
      // resolved slot names used by Mantle's FluidCuboid implementation.
      // TextureSlots also accepts the JSON reference form, but keeping the
      // runtime representation bare avoids mixing the two conventions.
      null, -1, face.flowing() ? "flowing_fluid" : "fluid",
      fluidUvs(from, to, direction, face.rotation(), face.flowing() ? 0.5f : 1f),
      quadrant(face.rotation()), data, new MutableObject<>()
    )));
    return new CuboidModelElement(from, to, bakedFaces, null, false, 0, data);
  }

  private static CuboidFace.UVs fluidUvs(Vector3fc from, Vector3fc to, Direction side, int rotation, float scale) {
    float u1;
    float u2;
    float v1;
    float v2;
    switch (side) {
      case DOWN -> { u1 = from.x(); v1 = 16f - to.z(); u2 = to.x(); v2 = 16f - from.z(); }
      case UP -> { u1 = from.x(); v1 = from.z(); u2 = to.x(); v2 = to.z(); }
      case SOUTH -> { u1 = from.x(); v1 = 16f - to.y(); u2 = to.x(); v2 = 16f - from.y(); }
      case WEST -> { u1 = from.z(); v1 = 16f - to.y(); u2 = to.z(); v2 = 16f - from.y(); }
      case EAST -> { u1 = 16f - to.z(); v1 = 16f - to.y(); u2 = 16f - from.z(); v2 = 16f - from.y(); }
      default -> { u1 = 16f - to.x(); v1 = 16f - to.y(); u2 = 16f - from.x(); v2 = 16f - from.y(); }
    }
    if (rotation >= 180) {
      float temp = v1;
      v1 = 16f - v2;
      v2 = 16f - temp;
    }
    if (rotation == 90 || rotation == 180) {
      float temp = u1;
      u1 = 16f - u2;
      u2 = 16f - temp;
    }
    if (rotation % 180 == 90) {
      return new CuboidFace.UVs(v1 * scale, u1 * scale, v2 * scale, u2 * scale);
    }
    return new CuboidFace.UVs(u1 * scale, v1 * scale, u2 * scale, v2 * scale);
  }

  private static com.mojang.math.Quadrant quadrant(int rotation) {
    return switch (rotation) {
      case 90 -> com.mojang.math.Quadrant.R90;
      case 180 -> com.mojang.math.Quadrant.R180;
      case 270 -> com.mojang.math.Quadrant.R270;
      default -> com.mojang.math.Quadrant.R0;
    };
  }

  private static final class FluidTextureBaked extends BaseBaked {
    private final FluidTextureUnbaked definition;
    private final Set<String> fluidSlots;
    private final Set<String> retexturedSlots;
    private final Map<FluidTextureKey,BlockStateModelPart> cache = new ConcurrentHashMap<>();

    private FluidTextureBaked(FluidTextureUnbaked definition, ModelBaker baker) {
      super(definition.model(), definition.modelState(), baker);
      this.definition = definition;
      this.fluidSlots = Set.copyOf(definition.fluidSlots());
      this.retexturedSlots = Set.copyOf(definition.retexturedSlots());
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      FluidTextureKey key = key(modelData(level, pos));
      if (key == null) {
        output.add(fallback);
      } else {
        output.add(cache.computeIfAbsent(key, this::bakeDynamic));
      }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      FluidTextureKey key = key(modelData(level, pos));
      return key == null ? fallback : key;
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      FluidTextureKey key = key(modelData(level, pos));
      return key == null ? fallback.particleMaterial() : cache.computeIfAbsent(key, this::bakeDynamic).particleMaterial();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      FluidTextureKey key = key(modelData(level, pos));
      return key == null ? fallback.materialFlags() : cache.computeIfAbsent(key, this::bakeDynamic).materialFlags();
    }

    @Nullable
    private FluidTextureKey key(ModelData data) {
      FluidStack fluid = data.get(ModelProperties.FLUID_STACK);
      if (fluid == null || fluid.isEmpty()) {
        fluid = FluidStack.EMPTY;
      } else {
        fluid = fluid.copy();
      }
      Block texture = retexturedSlots.isEmpty() ? null : data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (texture == null || texture == Blocks.AIR) {
        texture = null;
      }
      if (fluid.isEmpty() && texture == null) {
        return null;
      }
      return new FluidTextureKey(fluid, texture);
    }

    private BlockStateModelPart bakeDynamic(FluidTextureKey key) {
      if (geometry == null) {
        return fallback;
      }
      FluidStack fluid = key.fluid();
      Block texture = key.texture();
      boolean hasFluid = !fluid.isEmpty() && !fluidSlots.isEmpty();
      ExtraFaceData fluidData = hasFluid ? fluidFaceData(fluid) : null;
      Map<String,Material> replacements = new HashMap<>();
      if (texture != null) {
        Material textureMaterial = new Material(ModelHelper.getParticleTexture(texture));
        for (String slot : retexturedSlots) {
          if (baseTextures.getMaterial(slot) != null) {
            replacements.put(slot, textureMaterial);
          }
        }
      }
      if (hasFluid) {
        Material fluidMaterial = fluidMaterial(fluid);
        for (String slot : fluidSlots) {
          if (baseTextures.getMaterial(slot) != null) {
            replacements.put(slot, fluidMaterial);
          }
        }
      }
      if (replacements.isEmpty()) {
        return fallback;
      }
      List<CuboidModelElement> elements = new ArrayList<>(geometry.elements().size());
      for (int index = 0; index < geometry.elements().size(); index++) {
        CuboidModelElement element = geometry.elements().get(index);
        NativeBlockColorData colorData = NativeBlockColorData.at(definition.colors(), index);
        Map<Direction,CuboidFace> faces = new EnumMap<>(Direction.class);
        boolean fluidElement = false;
        for (Map.Entry<Direction,CuboidFace> entry : element.faces().entrySet()) {
          CuboidFace face = entry.getValue();
          boolean fluidFace = hasFluid && fluidSlots.contains(slot(face.texture()));
          fluidElement |= fluidFace;
          ExtraFaceData data = fluidFace ? fluidData : colorData.applyTo(face.faceData());
          faces.put(entry.getKey(), copyFace(face, data));
        }
        ExtraFaceData elementData = fluidElement ? fluidData : colorData.applyTo(element.faceData());
        elements.add(new CuboidModelElement(
          element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), elementData
        ));
      }
      return bake(elements, replacements);
    }

    private static ExtraFaceData fluidFaceData(FluidStack fluid) {
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      int color = fluidModel.fluidTintSource() == null
                  ? FluidTextureManager.getColor(fluid.getFluid().getFluidType())
                  : fluidModel.fluidTintSource().colorAsStack(fluid);
      int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
      return new ExtraFaceData(color == -1 ? 0xFFFFFFFF : color, luminosity, true);
    }

    private static Material fluidMaterial(FluidStack fluid) {
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      return new Material(fluidModel.stillMaterial().sprite().contents().name(), fluidModel.stillMaterial().forceTranslucent());
    }
  }

  /** Stable dynamic-texture key; FluidStack does not provide value equality in 26.1. */
  private static final class FluidTextureKey {
    private final FluidStack fluid;
    @Nullable
    private final Block texture;

    private FluidTextureKey(FluidStack fluid, @Nullable Block texture) {
      this.fluid = fluid;
      this.texture = texture;
    }

    private FluidStack fluid() {
      return fluid;
    }

    @Nullable
    private Block texture() {
      return texture;
    }

    @Override
    public boolean equals(Object object) {
      return this == object || object instanceof FluidTextureKey other
          && texture == other.texture
          && FluidStack.isSameFluidSameComponents(fluid, other.fluid);
    }

    @Override
    public int hashCode() {
      return 31 * FluidStack.hashFluidAndComponents(fluid) + System.identityHashCode(texture);
    }
  }

  private static final class MaterialBlockBaked extends BaseBaked {
    private final MaterialBlockUnbaked definition;
    private final Set<String> slots;
    private final Map<MaterialVariantId,BlockStateModelPart> cache = new ConcurrentHashMap<>();
    private final Map<MaterialVariantId,Integer> particleColors = new ConcurrentHashMap<>();
    private final Map<MaterialVariantId,TextureAtlasSprite> particleSprites = new ConcurrentHashMap<>();
    private final Map<Block,List<BlockStateModelPart>> blockCache = new ConcurrentHashMap<>();
    private final Map<Block,TextureAtlasSprite> blockParticleSprites = new ConcurrentHashMap<>();

    private MaterialBlockBaked(MaterialBlockUnbaked definition, ModelBaker baker) {
      super(definition.model(), definition.modelState(), baker);
      this.definition = definition;
      this.slots = Set.copyOf(definition.slots());
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      ModelData data = modelData(level, pos);
      Block texture = data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (texture != null && texture != Blocks.AIR && !slots.isEmpty()) {
        output.addAll(blockCache.computeIfAbsent(texture, this::bakeTexture));
        cacheBlockParticle(level, pos, texture);
        return;
      }
      MaterialVariantId material = data.get(ModelProperties.MATERIAL);
      if (material == null || IMaterial.UNKNOWN_ID.equals(material) || slots.isEmpty()) {
        output.add(fallback);
      } else {
        BlockStateModelPart part = cache.computeIfAbsent(material, this::bakeMaterial);
        output.add(part);
        cacheMaterialParticle(level, pos, material);
      }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      ModelData data = modelData(level, pos);
      Block texture = data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (texture != null && texture != Blocks.AIR && !slots.isEmpty()) {
        blockCache.computeIfAbsent(texture, this::bakeTexture);
        cacheBlockParticle(level, pos, texture);
        return texture;
      }
      MaterialVariantId material = data.get(ModelProperties.MATERIAL);
      if (material == null || IMaterial.UNKNOWN_ID.equals(material) || slots.isEmpty()) {
        return fallback;
      }
      BlockStateModelPart part = cache.computeIfAbsent(material, this::bakeMaterial);
      cacheMaterialParticle(level, pos, material);
      return material;
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      ModelData data = modelData(level, pos);
      Block texture = data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (texture != null && texture != Blocks.AIR && !slots.isEmpty()) {
        List<BlockStateModelPart> parts = blockCache.computeIfAbsent(texture, this::bakeTexture);
        cacheBlockParticle(level, pos, texture);
        return parts.getFirst().particleMaterial();
      }
      MaterialVariantId material = data.get(ModelProperties.MATERIAL);
      if (material == null || IMaterial.UNKNOWN_ID.equals(material) || slots.isEmpty()) {
        return fallback.particleMaterial();
      }
      BlockStateModelPart part = cache.computeIfAbsent(material, this::bakeMaterial);
      cacheMaterialParticle(level, pos, material);
      return part.particleMaterial();
    }

    private void cacheBlockParticle(BlockAndTintGetter level, BlockPos pos, Block texture) {
      TextureAtlasSprite sprite = blockParticleSprites.get(texture);
      if (sprite != null) {
        slimeknights.tconstruct.client.DynamicTableParticleExtensions.cacheParticleSprite(level, pos, sprite, -1);
      }
    }

    private void cacheMaterialParticle(BlockAndTintGetter level, BlockPos pos, MaterialVariantId material) {
      TextureAtlasSprite sprite = particleSprites.get(material);
      if (sprite != null) {
        slimeknights.tconstruct.client.DynamicTableParticleExtensions.cacheParticleSprite(
          level, pos, sprite, particleColors.getOrDefault(material, -1));
      }
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      ModelData data = modelData(level, pos);
      Block texture = data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (texture != null && texture != Blocks.AIR && !slots.isEmpty()) {
        return blockCache.computeIfAbsent(texture, this::bakeTexture).stream()
          .mapToInt(BlockStateModelPart::materialFlags).reduce(0, (left, right) -> left | right);
      }
      MaterialVariantId material = data.get(ModelProperties.MATERIAL);
      return material == null || IMaterial.UNKNOWN_ID.equals(material) || slots.isEmpty()
             ? fallback.materialFlags() : cache.computeIfAbsent(material, this::bakeMaterial).materialFlags();
    }

    /** Bakes the anvil-style variant using the block supplied by the block entity. */
    private List<BlockStateModelPart> bakeTexture(Block texture) {
      if (geometry == null) {
        return List.of(fallback);
      }
      List<Material> materials = MaterialBlockTextureHelper.getMaterials(texture);
      blockParticleSprites.put(texture, baker.materials().get(materials.getFirst(), resolved).sprite());
      List<BlockStateModelPart> parts = new ArrayList<>(materials.size());
      for (Material material : materials) {
        Map<String,Material> replacements = new HashMap<>();
        for (String slot : slots) {
          if (baseTextures.getMaterial(slot) != null) {
            replacements.put(slot, material);
          }
        }
        if (!replacements.isEmpty()) {
          parts.add(bake(copyElements(geometry.elements(), face -> face), replacements));
        }
      }
      return parts.isEmpty() ? List.of(fallback) : List.copyOf(parts);
    }

    private BlockStateModelPart bakeMaterial(MaterialVariantId material) {
      if (geometry == null) {
        return fallback;
      }
      Optional<MaterialRenderInfo> info = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      if (info.isEmpty()) {
        return fallback;
      }
      Map<String,Material> replacements = new HashMap<>();
      Map<String,ExtraFaceData> tints = new HashMap<>();
      int particleColor = -1;
      Function<Material,net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter = base -> baker.materials().get(base, resolved).sprite();
      for (String slot : slots) {
        Material base = baseTextures.getMaterial(slot);
        if (base == null) {
          continue;
        }
        MaterialRenderInfo.TintedSprite sprite = info.get().getSprite(base, spriteGetter);
        replacements.put(slot, new Material(sprite.sprite().contents().name()));
        tints.put(slot, new ExtraFaceData(sprite.color() == -1 ? 0xFFFFFFFF : sprite.color(), sprite.emissivity(), true));
        if ("particle".equals(slot)) {
          particleSprites.put(material, sprite.sprite());
          particleColor = sprite.color();
        }
      }
      if (replacements.isEmpty()) {
        return fallback;
      }
      particleColors.put(material, particleColor);
      List<CuboidModelElement> elements = copyElements(geometry.elements(), face -> {
        ExtraFaceData tint = tints.get(slot(face.texture()));
        return copyFace(face, tint);
      });
      return bake(elements, replacements);
    }
  }
}

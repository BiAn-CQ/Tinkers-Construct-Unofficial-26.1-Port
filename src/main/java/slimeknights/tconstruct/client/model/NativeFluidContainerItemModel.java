package slimeknights.tconstruct.client.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.fluid.texture.FluidTextureManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Native 26.1 replacement for Tinkers' fluid-container item loader. */
public final class NativeFluidContainerItemModel {
  private static final Identifier DEFAULT_PARENT = Identifier.withDefaultNamespace("item/generated");

  private NativeFluidContainerItemModel() {}

  /** Model definition corresponding to {@code loader: tconstruct:fluid_container}. */
  public record Unbaked(
      Identifier parent,
      Map<String,Material> textures,
      Optional<Identifier> fluid,
      boolean flipGas
  ) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("parent", DEFAULT_PARENT).forGetter(Unbaked::parent),
        com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, Material.CODEC)
            .optionalFieldOf("textures", Map.of()).forGetter(Unbaked::textures),
        Identifier.CODEC.optionalFieldOf("fluid").forGetter(Unbaked::fluid),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("flip_gas", true).forGetter(Unbaked::flipGas)
    ).apply(instance, Unbaked::new));

    public Unbaked {
      textures = Map.copyOf(textures);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      if (fluid.isPresent()) {
        net.minecraft.world.level.material.Fluid value = BuiltInRegistries.FLUID.getValue(fluid.get());
        if (value != null) {
          return bakeContainer(context, transformation, this, new FluidStack(value, FluidType.BUCKET_VOLUME));
        }
      }
      return new Dynamic(this, context, transformation);
    }

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }
  }

  private static ItemModel bakeContainer(ItemModel.BakingContext context, Matrix4fc transformation,
                                          Unbaked definition, FluidStack fluid) {
    ModelBaker baker = context.blockModelBaker();
    ResolvedModel parent = baker.getModel(definition.parent());
    Material base = firstMaterial(definition.textures(), parent, "base", "layer0");
    Material fluidMask = firstMaterial(definition.textures(), parent, "fluid", "layer1");

    QuadCollection.Builder quads = new QuadCollection.Builder();
    Material.Baked particle = null;
    if (base != null) {
      Material.Baked baked = baker.materials().get(base, parent);
      particle = baked;
      QuadCollection generated = baker.compute(new ItemModelGenerator.ItemLayerKey(
          baked, BlockModelRotation.IDENTITY, -1,
          new ExtraFaceData(0xFFFFFFFF, 0, true)
      ));
      generated.getAll().forEach(quads::addUnculledFace);
    }

    if (!fluid.isEmpty() && fluidMask != null) {
      Material.Baked baked = baker.materials().get(fluidMask, parent);
      if (particle == null) {
        particle = baked;
      }
      int color = fluidColor(fluid);
      int light = fluid.getFluid().getFluidType().getLightLevel(fluid);
      QuadCollection generated = baker.compute(new ItemModelGenerator.ItemLayerKey(
          baked, BlockModelRotation.IDENTITY, -1,
          new ExtraFaceData(color == -1 ? 0xFFFFFFFF : color, light, true)
      ));
      generated.getAll().forEach(quads::addUnculledFace);
    }

    if (particle == null) {
      particle = parent.resolveParticleMaterial(parent.getTopTextureSlots(), baker);
    }
    ModelRenderProperties parentProperties = ModelRenderProperties.fromResolvedModel(
        baker, parent, parent.getTopTextureSlots()
    );
    ModelRenderProperties properties = new ModelRenderProperties(
        false, particle, parentProperties.transforms()
    );
    Matrix4fc modelTransform = transformation;
    if (definition.flipGas() && !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir()) {
      Transformation flip = new Transformation(
          new Vector3f(), new Quaternionf(0, 0, 1, 0), new Vector3f(1, 1, 1), new Quaternionf()
      );
      modelTransform = Transformation.compose(transformation, Optional.of(flip));
    }
    return new CuboidItemModelWrapper(List.of(), quads.build(), properties, modelTransform);
  }

  private static Material firstMaterial(Map<String,Material> explicit, ResolvedModel parent, String... names) {
    for (String name : names) {
      Material material = explicit.get(name);
      if (material != null) {
        return material;
      }
      material = parent.getTopTextureSlots().getMaterial(name);
      if (material != null) {
        return material;
      }
    }
    return null;
  }

  private static int fluidColor(FluidStack fluid) {
    FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
    if (model.fluidTintSource() != null) {
      return model.fluidTintSource().colorAsStack(fluid);
    }
    return FluidTextureManager.getColor(fluid.getFluid().getFluidType());
  }

  private static final class Dynamic implements ItemModel {
    private final NativeFluidContainerItemModel.Unbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private final ItemModel emptyModel;
    private final Map<FluidKey,ItemModel> cache = new ConcurrentHashMap<>();

    private Dynamic(NativeFluidContainerItemModel.Unbaked definition, ItemModel.BakingContext context, Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
      this.emptyModel = bakeContainer(context, transformation, definition, FluidStack.EMPTY);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      Optional<FluidStack> contained = FluidUtil.getFluidContained(stack);
      if (contained.isEmpty() || contained.get().isEmpty()) {
        emptyModel.update(output, stack, resolver, displayContext, level, owner, seed);
        return;
      }
      FluidStack fluid = contained.get().copy();
      fluid.setAmount(FluidType.BUCKET_VOLUME);
      FluidKey key = new FluidKey(fluid);
      cache.computeIfAbsent(key, value -> bakeContainer(context, transformation, definition, value.fluid()))
          .update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }

  /** Stable key for a contained fluid; FluidStack itself is mutable and has identity equality. */
  private static final class FluidKey {
    private final FluidStack fluid;

    private FluidKey(FluidStack fluid) {
      this.fluid = fluid;
    }

    private FluidStack fluid() {
      return fluid;
    }

    @Override
    public boolean equals(Object object) {
      return this == object || object instanceof FluidKey other
          && FluidStack.isSameFluidSameComponents(fluid, other.fluid);
    }

    @Override
    public int hashCode() {
      return FluidStack.hashFluidAndComponents(fluid);
    }
  }
}

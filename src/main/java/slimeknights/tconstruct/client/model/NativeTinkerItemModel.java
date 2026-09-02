package slimeknights.tconstruct.client.model;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.apache.commons.lang3.mutable.MutableObject;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.client.model.util.MantleItemLayerGenerator;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.client.TConstructItemModelProperties;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.ModifierSetWorktableRecipe;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tools.modules.ranged.ammo.SmashingModule;
import slimeknights.tconstruct.tools.modules.cosmetic.BannerModule;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native 26.1 item-model bridge for the two material-driven item loaders used
 * by Tinkers' Construct.  It deliberately keeps the old JSON data format at
 * the resource boundary, but bakes into the new item render state API.
 */
public final class NativeTinkerItemModel implements ItemModel {
  private static final Identifier DEFAULT_PARENT = Identifier.withDefaultNamespace("item/generated");
  private static final Set<ItemDisplayContext> SMALL_MODEL_CONTEXTS = ConcurrentHashMap.newKeySet();

  private final ItemModel smallModel;
  @Nullable
  private final ItemModel largeModel;
  /**
   * GUI models in the legacy tool renderer only contained the front-facing
   * item-layer quads.  Keeping that model separate prevents the new native
   * item renderer from drawing the generated side/back quads over machine
   * slots, which otherwise appears as a black frame or an empty item.
   */
  private final ItemModel guiModel;

  private NativeTinkerItemModel(ItemModel smallModel, @Nullable ItemModel largeModel, ItemModel guiModel) {
    this.smallModel = smallModel;
    this.largeModel = largeModel;
    this.guiModel = guiModel;
  }

  @Override
  public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                     ItemDisplayContext displayContext, @Nullable ClientLevel level,
                     @Nullable ItemOwner owner, int seed) {
    ItemModel selected = displayContext == ItemDisplayContext.GUI
      ? guiModel
      : largeModel != null && usesLargeModel(displayContext) ? largeModel : smallModel;
    selected.update(output, item, resolver, displayContext, level, owner, seed);
  }

  /** Registers a display context that should use compact rather than doubled tool geometry. */
  public static void registerSmallModelContext(ItemDisplayContext context) {
    SMALL_MODEL_CONTEXTS.add(context);
  }

  /** Large geometry is used except in GUI and contexts registered for the compact tool model. */
  private static boolean usesLargeModel(ItemDisplayContext context) {
    return context != ItemDisplayContext.GUI && !SMALL_MODEL_CONTEXTS.contains(context);
  }

  /** One compact item-property override entry. */
  public record PropertyOverride(Map<String, Float> predicate, ItemModel.Unbaked model) {
    public static final Codec<PropertyOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("predicate").forGetter(PropertyOverride::predicate),
      ItemModels.CODEC.fieldOf("model").forGetter(PropertyOverride::model)
    ).apply(instance, PropertyOverride::new));
  }

  /**
   * Compact native item-property override model.
   *
   * <p>The vanilla 26.1 condition/range/select nodes are excellent for
   * authored definitions, but expanding every override into a nested
   * fallback tree duplicates the base tool model many times.  This compact
   * adapter keeps the same native item-model lifecycle and property values
   * while baking each model exactly once.</p>
   */
  public record PropertyOverridesUnbaked(ItemModel.Unbaked base, List<PropertyOverride> overrides)
      implements ItemModel.Unbaked {
    public static final MapCodec<PropertyOverridesUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      ItemModels.CODEC.fieldOf("base").forGetter(PropertyOverridesUnbaked::base),
      PropertyOverride.CODEC.listOf().fieldOf("overrides").forGetter(PropertyOverridesUnbaked::overrides)
    ).apply(instance, PropertyOverridesUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      base.resolveDependencies(resolver);
      overrides.forEach(override -> override.model().resolveDependencies(resolver));
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      ItemModel bakedBase = base.bake(context, transformation);
      List<ItemModel> bakedOverrides = overrides.stream()
        .map(override -> override.model().bake(context, transformation))
        .toList();
      return new PropertyOverridesModel(bakedBase, overrides, bakedOverrides);
    }

    @Override
    public MapCodec<PropertyOverridesUnbaked> type() {
      return MAP_CODEC;
    }
  }

  private static final class PropertyOverridesModel implements ItemModel {
    private final ItemModel base;
    private final List<PropertyOverride> overrides;
    private final List<ItemModel> bakedOverrides;

    private PropertyOverridesModel(ItemModel base, List<PropertyOverride> overrides, List<ItemModel> bakedOverrides) {
      this.base = base;
      this.overrides = overrides;
      this.bakedOverrides = bakedOverrides;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      ItemModel selected = base;
      for (int index = 0; index < overrides.size(); index++) {
        PropertyOverride override = overrides.get(index);
        boolean matches = true;
        for (Map.Entry<String, Float> predicate : override.predicate().entrySet()) {
          if (TConstructItemModelProperties.getValue(predicate.getKey(), stack, level, owner, displayContext, seed)
              < predicate.getValue()) {
            matches = false;
            break;
          }
        }
        if (matches) {
          selected = bakedOverrides.get(index);
        }
      }
      selected.update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }

  /** Runtime inputs for the legacy modifier overlay map, captured at bake time. */
  private record ModifierRenderData(Map<Identifier, NativeModifierModel> modifiers, List<NativeModifierModel> constants,
                                    List<Identifier> firstModifiers, boolean showTraits, IToolStackView tool) {}

  /**
   * Adds the texture layers described by a legacy modifier map to a native
   * cuboid item model.  The old baked-model implementation performed this in
   * Forge's {@code ItemOverrides}; 26.1 has no equivalent callback, so the
   * dynamic tool cache bakes the selected layers once per tool component set.
   */
  private static void addModifierQuads(ModelBaker baker, ResolvedModel parent,
                                       List<QuadCollection> reversedLayers, ItemLayerPixels usedPixels,
                                       ModifierRenderData data, boolean large) {
    ModifierNBT active = data.showTraits() ? data.tool().getModifiers() : data.tool().getUpgrades();
    List<ModifierEntry> activeList = new ArrayList<>();
    Map<Identifier, ModifierEntry> activeEntries = new HashMap<>();
    for (ModifierEntry entry : active) {
      activeList.add(entry);
      activeEntries.put(entry.getId().location(), entry);
    }
    Set<Identifier> first = new HashSet<>(data.firstModifiers());
    Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(
      data.tool().getPersistentData(), TConstruct.getResource("invisible_modifiers"));

    // Generate from visually highest to lowest, then reverse the finished collection once.
    for (int index = activeList.size() - 1; index >= 0; index--) {
      ModifierEntry entry = activeList.get(index);
      Identifier id = entry.getId().location();
      if (!first.contains(id) && !hidden.contains(entry.getId())) {
        NativeModifierModel definition = data.modifiers().get(id);
        if (definition != null) {
          addModifierDefinition(baker, parent, reversedLayers, usedPixels,
            definition.definition(), entry, data.tool(), large);
        }
      }
    }
    for (int index = data.firstModifiers().size() - 1; index >= 0; index--) {
      Identifier id = data.firstModifiers().get(index);
      ModifierEntry entry = activeEntries.get(id);
      if (entry != null) {
        NativeModifierModel definition = data.modifiers().get(id);
        if (definition != null) {
          addModifierDefinition(baker, parent, reversedLayers, usedPixels,
            definition.definition(), entry, data.tool(), large);
        }
      }
    }
    // Constant entries are generally trait overlays.  A tconstruct:trait
    // wrapper supplies its own modifier condition; entries without a condition
    // are genuinely unconditional.
    for (NativeModifierModel definition : data.constants()) {
      addModifierDefinition(baker, parent, reversedLayers, usedPixels,
        definition.definition(), null, data.tool(), large);
    }
  }

  /** Resolves a modifier definition that was validated and typed by the item-model codec. */
  private static void addModifierDefinition(ModelBaker baker, ResolvedModel parent,
                                             List<QuadCollection> reversedLayers, ItemLayerPixels usedPixels,
                                             NativeModifierModel.Definition definition,
                                             @Nullable ModifierEntry entry, IToolStackView tool, boolean large) {
    if (definition instanceof NativeModifierModel.Empty) {
      return;
    }
    if (definition instanceof NativeModifierModel.Compound compound) {
      for (NativeModifierModel.Definition child : compound.models()) {
        addModifierDefinition(baker, parent, reversedLayers, usedPixels, child, entry, tool, large);
      }
      return;
    }
    if (definition instanceof NativeModifierModel.Crafted craftedDefinition) {
      ModifierEntry crafted = tool.getUpgrades().getEntry(new ModifierId(craftedDefinition.modifier()));
      if (crafted.getLevel() > 0) {
        addModifierDefinition(baker, parent, reversedLayers, usedPixels,
          craftedDefinition.model(), crafted, tool, large);
      }
      return;
    }
    if (definition instanceof NativeModifierModel.Trait traitDefinition) {
      ModifierEntry trait = getTraitEntry(tool, traitDefinition.modifier());
      if (trait != null && trait.getLevel() > 0) {
        addModifierDefinition(baker, parent, reversedLayers, usedPixels,
          traitDefinition.model(), trait, tool, large);
      }
      return;
    }
    if (definition instanceof NativeModifierModel.MaterialFallback fallback) {
      boolean hasFallback = fallback.index() < tool.getMaterials().size()
                             && MaterialRenderInfoLoader.INSTANCE.hasFallback(
                               tool.getMaterials().get(fallback.index()).getVariant(), fallback.fallbacks());
      addModifierDefinition(baker, parent, reversedLayers, usedPixels,
        hasFallback ? fallback.ifTrue() : fallback.ifFalse(), entry, tool, large);
      return;
    }
    if (definition instanceof NativeModifierModel.PersistentMaterial materialDefinition) {
      if (entry == null) {
        return;
      }
      Identifier key = materialDefinition.key() == null ? entry.getId().location() : materialDefinition.key();
      MaterialVariantId material = MaterialVariantId.tryParse(tool.getPersistentData().getStringOr(key, ""));
      if (material != null) {
        Identifier texture = large && materialDefinition.textureLarge() != null
                             ? materialDefinition.textureLarge() : materialDefinition.texture();
        addMaterialModifierTexture(baker, parent, reversedLayers, usedPixels, texture, material, null);
      }
      return;
    }
    if (definition instanceof NativeModifierModel.Slimeskull slimeskull) {
      if (slimeskull.skullIndex() >= tool.getMaterials().size()
          || slimeskull.slimeIndex() >= tool.getMaterials().size()) {
        return;
      }
      int color = MaterialRenderInfoLoader.INSTANCE
        .getRenderInfo(tool.getMaterials().get(slimeskull.slimeIndex()).getVariant())
        .map(MaterialRenderInfo::vertexColor)
        .orElse(-1);
      Identifier dyed = TConstruct.getResource("dyed");
      if (tool.getPersistentData().contains(dyed)) {
        color = 0xFF000000 | tool.getPersistentData().getIntOr(dyed, 0);
      }
      addMaterialModifierTexture(baker, parent, reversedLayers, usedPixels, slimeskull.texture(),
        tool.getMaterials().get(slimeskull.skullIndex()).getVariant(), color);
      return;
    }
    if (definition instanceof NativeModifierModel.Texture textureDefinition) {
      Identifier texture = large && textureDefinition.textureLarge() != null
                           ? textureDefinition.textureLarge() : textureDefinition.texture();
      int color = textureDefinition.color();
      if (textureDefinition.kind() == NativeModifierModel.TextureKind.DYED) {
        if (entry == null || !tool.getPersistentData().contains(entry.getId().location())) {
          return;
        }
        color = 0xFF000000 | tool.getPersistentData().getIntOr(entry.getId().location(), 0);
      } else if (textureDefinition.kind() == NativeModifierModel.TextureKind.MATERIAL) {
        if (entry == null) {
          return;
        }
        MaterialVariantId material = MaterialVariantId.tryParse(
          tool.getPersistentData().getStringOr(entry.getId().location(), ""));
        if (material == null) {
          return;
        }
        addMaterialModifierTexture(baker, parent, reversedLayers, usedPixels, texture, material,
          color == -1 ? null : color);
        return;
      } else if (textureDefinition.kind() == NativeModifierModel.TextureKind.POTION) {
        if (entry == null) {
          return;
        }
        Identifier potionId = tryParseIdentifier(tool.getPersistentData().getStringOr(entry.getId().location(), ""));
        if (potionId == null) {
          return;
        }
        Optional<Integer> potionColor = BuiltInRegistries.POTION.get(potionId)
          .map(potion -> 0xFF000000 | new PotionContents(potion).getColor());
        if (potionColor.isEmpty()) {
          return;
        }
        color = potionColor.get();
      }
      addModifierTexture(baker, parent, reversedLayers, usedPixels,
        texture, color, textureDefinition.luminosity());
      return;
    }
    if (definition instanceof NativeModifierModel.Fluid fluidDefinition) {
      ToolTankHelper tankHelper = getTankHelper(fluidDefinition.tankHelper());
      FluidStack fluid = tankHelper.getFluid(tool);
      if (fluid.isEmpty()) {
        return;
      }
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      int color = fluidModel.fluidTintSource() == null
              ? FluidTextureManager.getColor(fluid.getFluid().getFluidType())
              : fluidModel.fluidTintSource().colorAsStack(fluid);
      Identifier texture = large && fluidDefinition.textureLarge() != null
                           ? fluidDefinition.textureLarge() : fluidDefinition.texture();
      if (texture == null) {
        texture = large && fluidDefinition.maskLarge() != null
                  ? fluidDefinition.maskLarge() : fluidDefinition.mask();
      }
      if (texture != null) {
        addFluidModifierTexture(baker, parent, reversedLayers, texture, fluidModel.stillMaterial(), color,
          fluid.getFluid().getFluidType().getLightLevel(fluid));
      }
      return;
    }
    if (definition instanceof NativeModifierModel.Tank tankDefinition) {
      ToolTankHelper tankHelper = getTankHelper(tankDefinition.tankHelper());
      FluidStack fluid = tankHelper.getFluid(tool);
      if (fluid.isEmpty()) {
        return;
      }
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      int color = fluidModel.fluidTintSource() == null
                  ? FluidTextureManager.getColor(fluid.getFluid().getFluidType())
                  : fluidModel.fluidTintSource().colorAsStack(fluid);
      Identifier texture = large && tankDefinition.textureLarge() != null
                           ? tankDefinition.textureLarge() : tankDefinition.texture();
      if (texture == null) {
        Identifier full = large && tankDefinition.fullLarge() != null
                          ? tankDefinition.fullLarge() : tankDefinition.full();
        Identifier partial = large && tankDefinition.partialLarge() != null
                             ? tankDefinition.partialLarge() : tankDefinition.partial();
        texture = full != null && fluid.getAmount() + tankDefinition.tolerance() >= tankHelper.getCapacity(tool)
                  ? full : partial;
      }
      if (texture == null) {
        texture = large && tankDefinition.maskLarge() != null
                  ? tankDefinition.maskLarge() : tankDefinition.mask();
      }
      if (texture != null) {
        addFluidModifierTexture(baker, parent, reversedLayers, texture, fluidModel.stillMaterial(), color,
          fluid.getFluid().getFluidType().getLightLevel(fluid));
      }
      return;
    }
    if (definition instanceof NativeModifierModel.Banner banner) {
      addBannerModifierTextures(baker, parent, reversedLayers, banner, entry, tool, large);
      return;
    }
    if (definition instanceof NativeModifierModel.ArmorTrim trim) {
      if (entry != null && !large && tool.getItem() instanceof ModifiableArmorItem) {
        addTrimModifierTexture(baker, parent, reversedLayers, usedPixels, entry, tool,
          Identifier.withDefaultNamespace("trims/items/" + trim.slot() + "_trim"));
      }
      return;
    }
    if (definition instanceof NativeModifierModel.CustomTrim trim) {
      if (entry != null) {
        Identifier root = large && trim.rootLarge() != null ? trim.rootLarge() : trim.root();
        addTrimModifierTexture(baker, parent, reversedLayers, usedPixels, entry, tool, root);
      }
    }
  }

  /** Gets the trait entry only when the same modifier is not installed as an upgrade. */
  private static ModifierEntry getTraitEntry(IToolStackView tool, Identifier id) {
    ModifierId modifier = new ModifierId(id);
    if (tool.getUpgrades().getLevel(modifier) == 0) {
      return tool.getModifier(modifier);
    }
    return ModifierEntry.EMPTY;
  }

  /** Resolves the helper used by a fluid modifier overlay. */
  private static ToolTankHelper getTankHelper(@Nullable Identifier id) {
    return TConstruct.getResource("smashing").equals(id) ? SmashingModule.TANK_HELPER : ToolTankHelper.TANK_HELPER;
  }

  @Nullable
  private static Identifier tryParseIdentifier(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return Identifier.parse(value);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  /** Bakes a modifier texture using the same item-layer path as tool parts. */
  private static void addModifierTexture(ModelBaker baker, ResolvedModel parent,
                                         List<QuadCollection> reversedLayers, ItemLayerPixels usedPixels,
                                         Identifier texture, int color, int luminosity) {
    addModifierBakedTexture(baker, reversedLayers, usedPixels,
      baker.materials().get(new Material(texture), parent), color, luminosity);
  }

  /** Resolves a material-aware texture and optionally overrides its normal vertex tint. */
  private static void addMaterialModifierTexture(ModelBaker baker, ResolvedModel parent,
                                                 List<QuadCollection> reversedLayers,
                                                 @Nullable ItemLayerPixels usedPixels,
                                                 Identifier texture, MaterialVariantId material,
                                                 @Nullable Integer colorOverride) {
    Material base = new Material(texture);
    Material.Baked baseBaked = baker.materials().get(base, parent);
    Optional<MaterialRenderInfo> renderInfo = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (renderInfo.isPresent()) {
      MaterialRenderInfo.TintedSprite sprite = renderInfo.get().getSprite(
        base, materialRef -> baker.materials().get(materialRef, parent).sprite());
      int color = colorOverride == null ? sprite.color() : colorOverride;
      addModifierBakedTexture(baker, reversedLayers, usedPixels,
        new Material.Baked(sprite.sprite(), baseBaked.forceTranslucent()), color, sprite.emissivity());
    } else {
      addModifierTexture(baker, parent, reversedLayers, usedPixels, texture,
        colorOverride == null ? -1 : colorOverride, 0);
    }
  }

  /** Bakes an already resolved sprite as a complete item-layer overlay. */
  private static void addModifierBakedTexture(ModelBaker baker, List<QuadCollection> reversedLayers,
                                              @Nullable ItemLayerPixels usedPixels,
                                              Material.Baked baked, int color, int luminosity) {
    reversedLayers.add(bakeModifierBakedTexture(baker, usedPixels, baked, color, luminosity));
  }

  private static QuadCollection bakeModifierBakedTexture(ModelBaker baker,
                                                          @Nullable ItemLayerPixels usedPixels,
                                                          Material.Baked baked, int color, int luminosity) {
    int argb = color == -1 ? 0xFFFFFFFF : color;
    ExtraFaceData faceData = new ExtraFaceData(argb, Math.max(0, Math.min(15, luminosity)), true);
    return MantleItemLayerGenerator.bake(
      baker, baked, BlockModelRotation.IDENTITY, -1, faceData, usedPixels);
  }

  /** Small depth offset copied from NeoForge's dynamic fluid container model. */
  private static final Transformation FLUID_MODIFIER_TRANSFORM = new Transformation(
    new Vector3f(), new org.joml.Quaternionf(), new Vector3f(1, 1, 1.002f), new org.joml.Quaternionf());

  /** Bakes a real fluid sprite in the opaque pixels of a modifier mask. */
  private static void addFluidModifierTexture(ModelBaker baker, ResolvedModel parent,
                                              List<QuadCollection> reversedLayers,
                                              Identifier maskId, Material.Baked fluid, int color, int luminosity) {
    Material.Baked mask = baker.materials().get(new Material(maskId), parent);
    if (isMissing(mask) || isMissing(fluid)) {
      return;
    }
    ExtraFaceData faceData = new ExtraFaceData(color == -1 ? 0xFFFFFFFF : color,
      Math.max(0, Math.min(15, luminosity)), true);
    QuadCollection generated = UnbakedElementsHelper.bakeItemMaskQuads(
      baker, -1, mask, fluid,
      new ComposedModelState(BlockModelRotation.IDENTITY, FLUID_MODIFIER_TRANSFORM), faceData);
    reversedLayers.add(generated);
  }

  /** Adds all stored banner layers using the generated Tinkers banner sprites. */
  private static void addBannerModifierTextures(ModelBaker baker, ResolvedModel parent,
                                                 List<QuadCollection> reversedLayers,
                                                 NativeModifierModel.Banner definition, @Nullable ModifierEntry entry,
                                                 IToolStackView tool, boolean large) {
    if (entry == null) {
      return;
    }
    Identifier prefix = large && definition.prefixLarge() != null ? definition.prefixLarge() : definition.prefix();
    if (prefix == null) {
      return;
    }
    net.minecraft.nbt.ListTag patterns = tool.getPersistentData()
      .getList(BannerModule.patternKey(entry.getId()), net.minecraft.nbt.ListTag.TAG_COMPOUND);
    for (int i = 0; i < patterns.size(); i++) {
      net.minecraft.nbt.CompoundTag tag = patterns.getCompoundOrEmpty(i);
      Identifier pattern = BannerModule.getAssetId(tag.getStringOr(BannerModule.KEY_PATTERN, ""));
      if (pattern == null) {
        continue;
      }
      Identifier texture = prefix.withSuffix(MaterialRenderInfo.getSuffix(pattern));
      Material.Baked baked = baker.materials().get(new Material(texture), parent);
      if (!isMissing(baked)) {
        int patternColor = 0xFF000000 | tag.getIntOr(BannerModule.KEY_COLOR, 0);
        // Legacy banner overlays were GUI-style front quads and intentionally
        // did not participate in the side-face pixel map.
        reversedLayers.add(frontOnly(bakeModifierBakedTexture(
          baker, null, baked, patternColor, 0)));
      }
    }
  }

  /** Adds an armor/custom trim, using a tinted base sprite if the palette sprite is absent. */
  private static void addTrimModifierTexture(ModelBaker baker, ResolvedModel parent,
                                             List<QuadCollection> reversedLayers,
                                             ItemLayerPixels usedPixels,
                                             ModifierEntry entry, IToolStackView tool, Identifier root) {
    String materialId = tool.getPersistentData().getString(TrimModule.materialKey(entry.getId()));
    Identifier materialLocation = tryParseIdentifier(materialId);
    if (materialLocation == null) {
      return;
    }
    ClientLevel level = Minecraft.getInstance().level;
    if (level == null) {
      return;
    }
    TrimMaterial material = level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(materialLocation)
      .map(Holder::value).orElse(null);
    if (material == null) {
      return;
    }
    Identifier materialTexture = root.withSuffix("_" + material.assets().base().suffix());
    Material.Baked baked = baker.materials().get(new Material(materialTexture), parent);
    int color = -1;
    if (isMissing(baked)) {
      baked = baker.materials().get(new Material(root), parent);
      TextColor textColor = material.description().getStyle().getColor();
      if (textColor != null) {
        color = textColor.getValue() | 0xFF000000;
      }
    }
    if (!isMissing(baked)) {
      addModifierBakedTexture(baker, reversedLayers, usedPixels, baked, color, 0);
    }
  }

  private static boolean isMissing(Material.Baked material) {
    return MissingTextureAtlasSprite.getLocation().equals(material.sprite().contents().name());
  }

  /** Shared model bake for a tool with several material-bearing parts. */
  private static NativeTinkerItemModel bakeTool(ItemModel.BakingContext context, Matrix4fc rootTransform,
                                                 Identifier parentId, Map<String, Material> textures,
                                                 List<Part> parts, List<MaterialVariantId> materials,
                                                 boolean large, Vector2fc largeOffset,
                                                 Map<Identifier, NativeModifierModel> modifierModels,
                                                 List<NativeModifierModel> modifierConstants, List<Identifier> firstModifiers,
                                                 boolean showTraits, IToolStackView tool) {
    ResolvedModel parent = context.blockModelBaker().getModel(parentId);
    List<Part> resolvedParts = parts.isEmpty() ? List.of(new Part("tool", -1)) : parts;
    ModifierRenderData modifierData = new ModifierRenderData(modifierModels, modifierConstants, firstModifiers, showTraits, tool);
    VariantModels small = bakeVariant(context, parent, rootTransform, textures, resolvedParts, materials, false, new Vector2f(), modifierData);
    if (!large) {
      return new NativeTinkerItemModel(small.all(), null, small.gui());
    }
    VariantModels big = bakeVariant(context, parent, rootTransform, textures, resolvedParts, materials, true, largeOffset, modifierData);
    return new NativeTinkerItemModel(small.all(), big.all(), small.gui());
  }

  /** Shared model bake for a single material part item. */
  private static NativeTinkerItemModel bakeMaterial(ItemModel.BakingContext context, Matrix4fc rootTransform,
                                                     Identifier parentId, Material texture, MaterialVariantId material,
                                                     Vector2fc offset) {
    ResolvedModel parent = context.blockModelBaker().getModel(parentId);
    Part part = new Part("texture", 0);
    VariantModels small = bakeVariant(context, parent, rootTransform, Map.of("texture", texture), List.of(part),
                                      List.of(material), false, offset, null);
    return new NativeTinkerItemModel(small.all(), null, small.gui());
  }

  /**
   * Bakes an item form of a material block, such as a material anvil.
   *
   * <p>The block-state model receives its material through {@code ModelData},
   * while an item receives it from the stack's material component/custom data.
   * These are separate render paths in 26.1, so the item path needs its own
   * small cache instead of relying on the block-state model registration.</p>
   */
  private static ItemModel bakeMaterialBlock(ItemModel.BakingContext context, Matrix4fc transformation,
                                              MaterialBlockUnbaked definition, @Nullable Block textureBlock,
                                              MaterialVariantId material) {
    if (textureBlock != null && textureBlock != Blocks.AIR) {
      List<Material> materials = MaterialBlockTextureHelper.getMaterials(textureBlock);
      if (materials.size() > 1) {
        return new CompositeModel(materials.stream()
          .map(texture -> bakeMaterialBlockLayer(context, transformation, definition, texture, IMaterial.UNKNOWN_ID))
          .toList());
      }
      return bakeMaterialBlockLayer(context, transformation, definition, materials.getFirst(), IMaterial.UNKNOWN_ID);
    }
    return bakeMaterialBlockLayer(context, transformation, definition, null, material);
  }

  /** Bakes one texture layer of a material block. */
  private static ItemModel bakeMaterialBlockLayer(ItemModel.BakingContext context, Matrix4fc transformation,
                                                   MaterialBlockUnbaked definition, @Nullable Material textureMaterial,
                                                   MaterialVariantId material) {
    ModelBaker baker = context.blockModelBaker();
    ResolvedModel resolved = baker.getModel(definition.model());
    ModelRenderProperties parentProperties = ModelRenderProperties.fromResolvedModel(
      baker, resolved, resolved.getTopTextureSlots());
    if (!(resolved.getTopGeometry() instanceof UnbakedCuboidGeometry geometry)) {
      // All current material blocks are cuboid models.  Keep a safe empty
      // model for malformed third-party variants rather than crashing the
      // entire client during model baking.
      return new CuboidItemModelWrapper(List.of(), new QuadCollection.Builder().build(), parentProperties, transformation);
    }

    TextureSlots baseTextures = resolved.getTopTextureSlots();
    TextureSlots.Data baseTextureData = copyMaterialBlockTextureData(geometry, baseTextures);
    Map<String,Material> replacements = new HashMap<>();
    Map<String,ExtraFaceData> tints = new HashMap<>();

    if (textureMaterial != null) {
      for (String slot : definition.slots()) {
        String cleanSlot = stripTextureReference(slot);
        if (baseTextures.getMaterial(cleanSlot) != null) {
          replacements.put(cleanSlot, textureMaterial);
        }
      }
    } else if (!IMaterial.UNKNOWN_ID.equals(material)) {
      Optional<MaterialRenderInfo> renderInfo = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      if (renderInfo.isPresent()) {
        for (String slot : definition.slots()) {
          String cleanSlot = stripTextureReference(slot);
          Material base = baseTextures.getMaterial(cleanSlot);
          if (base == null) {
            continue;
          }
          MaterialRenderInfo.TintedSprite sprite = renderInfo.get().getSprite(
            base, materialRef -> baker.materials().get(materialRef, resolved).sprite());
          replacements.put(cleanSlot, new Material(sprite.sprite().contents().name()));
          tints.put(cleanSlot, new ExtraFaceData(
            sprite.color() == -1 ? 0xFFFFFFFF : sprite.color(), sprite.emissivity(), true));
        }
      }
    } else {
      // An empty material-block stack is the base/unknown variant.  Keep the
      // model's real fallback texture instead of asking the material renderer
      // for a generated "*_tconstruct_unknown" sprite.  Storage-block
      // unknowns do not have a generated palette in the staged 26.1 runtime,
      // while the model already carries the safe fallback texture.
    }

    List<CuboidModelElement> elements = copyMaterialBlockElements(geometry.elements(), tints);
    TextureSlots.Data.Builder overrideData = new TextureSlots.Data.Builder();
    replacements.forEach(overrideData::addTexture);
    TextureSlots textures = new TextureSlots.Resolver()
      // Resolve the base first so the dynamic material wins over the model's
      // original texture for the selected slots.
      .addLast(overrideData.build())
      .addLast(baseTextureData)
      .resolve(resolved);
    QuadCollection quads = new UnbakedCuboidGeometry(elements).bake(
      textures, baker, BlockModelRotation.IDENTITY, resolved, resolved.getTopAdditionalProperties());
    Material.Baked particle = resolved.resolveParticleMaterial(textures, baker);
    ModelRenderProperties properties = new ModelRenderProperties(
      parentProperties.usesBlockLight(), particle, parentProperties.transforms());
    return new CuboidItemModelWrapper(List.of(), quads, properties, transformation);
  }

  /** Copies only texture slots referenced by the cuboid geometry and particle. */
  private static TextureSlots.Data copyMaterialBlockTextureData(UnbakedCuboidGeometry geometry, TextureSlots baseTextures) {
    TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
    for (CuboidModelElement element : geometry.elements()) {
      for (CuboidFace face : element.faces().values()) {
        String slot = stripTextureReference(face.texture());
        Material material = baseTextures.getMaterial(slot);
        if (material != null) {
          builder.addTexture(slot, material);
        }
      }
    }
    Material particle = baseTextures.getMaterial("particle");
    if (particle != null) {
      builder.addTexture("particle", particle);
    }
    return builder.build();
  }

  private static List<CuboidModelElement> copyMaterialBlockElements(List<CuboidModelElement> source,
                                                                      Map<String,ExtraFaceData> tints) {
    List<CuboidModelElement> result = new ArrayList<>(source.size());
    for (CuboidModelElement element : source) {
      Map<Direction,CuboidFace> faces = new EnumMap<>(Direction.class);
      element.faces().forEach((direction, face) -> {
        ExtraFaceData tint = tints.get(stripTextureReference(face.texture()));
        faces.put(direction, copyMaterialBlockFace(face, tint));
      });
      result.add(new CuboidModelElement(
        element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), element.faceData()));
    }
    return result;
  }

  private static CuboidFace copyMaterialBlockFace(CuboidFace face, @Nullable ExtraFaceData data) {
    return new CuboidFace(
      face.cullForDirection(), face.tintIndex(), face.texture(), face.uvs(), face.rotation(),
      data == null ? face.faceData() : data, new MutableObject<>());
  }

  private static String stripTextureReference(String texture) {
    return texture.startsWith("#") ? texture.substring(1) : texture;
  }

  /** Baked small/large geometry plus the front-only model used by GUI slots. */
  private record VariantModels(ItemModel all, ItemModel gui) {}

  /** Bakes a material sequence into a single composite quad collection. */
  private static VariantModels bakeVariant(ItemModel.BakingContext context, ResolvedModel parent, Matrix4fc rootTransform,
                                           Map<String, Material> textures, List<Part> parts,
                                           List<MaterialVariantId> materials, boolean large, Vector2fc offset,
                                           @Nullable ModifierRenderData modifierData) {
    ModelBaker baker = context.blockModelBaker();
    ModelRenderProperties parentProperties = ModelRenderProperties.fromResolvedModel(baker, parent, parent.getTopTextureSlots());
    QuadCollection.Builder quads = new QuadCollection.Builder();
    List<QuadCollection> reversedLayers = new ArrayList<>();
    ItemLayerPixels usedPixels = new ItemLayerPixels();
    List<ToolPartLayer> partLayers = new ArrayList<>();
    Material.Baked particle = null;
    for (Part part : parts) {
      Material base = large ? textures.get("large_" + part.name()) : null;
      if (base == null) {
        base = textures.get(part.name());
      }
      if (base == null) {
        continue;
      }

      Material.Baked bakedBase = baker.materials().get(base, parent);
      Material.Baked rendered = bakedBase;
      int color = 0xFFFFFFFF;
      int luminosity = 0;
      if (part.index() >= 0) {
        MaterialVariantId material = part.index() < materials.size() ? materials.get(part.index()) : IMaterial.UNKNOWN_ID;
        Optional<MaterialRenderInfo> renderInfo = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
        if (renderInfo.isPresent()) {
          MaterialRenderInfo.TintedSprite sprite = renderInfo.get().getSprite(
            base, materialRef -> baker.materials().get(materialRef, parent).sprite());
          rendered = new Material.Baked(sprite.sprite(), bakedBase.forceTranslucent());
          color = sprite.color() == -1 ? 0xFFFFFFFF : sprite.color();
          luminosity = sprite.emissivity();
        }
      }
      if (particle == null) {
        particle = rendered;
      }

      // A negative layer index disables runtime tint lookup.  Material color
      // and light emission are baked into the quad by ExtraFaceData instead.
      ExtraFaceData faceData = new ExtraFaceData(color, luminosity, true);
      partLayers.add(new ToolPartLayer(rendered, faceData));
    }

    if (modifierData != null && (!modifierData.modifiers().isEmpty() || !modifierData.constants().isEmpty())) {
      addModifierQuads(baker, parent, reversedLayers, usedPixels, modifierData, large);
    }
    addToolPartLayers(baker, reversedLayers, partLayers, usedPixels);
    for (int index = reversedLayers.size() - 1; index >= 0; index--) {
      reversedLayers.get(index).getAll().forEach(quads::addUnculledFace);
    }

    if (particle == null) {
      particle = parentProperties.particleMaterial();
    }
    ModelRenderProperties properties = new ModelRenderProperties(
      parentProperties.usesBlockLight(), particle, parentProperties.transforms());
    Matrix4fc transform = rootTransform;
    if (large) {
      Transformation geometry = new Transformation(
        new Vector3f((offset.x() - 8) / 32f, (-offset.y() - 8) / 32f, 0),
        null, new Vector3f(2, 2, 1), null);
      transform = Transformation.compose(transform, Optional.of(geometry));
    } else if (offset.x() != 0 || offset.y() != 0) {
      Transformation itemOffset = new Transformation(
        new Vector3f(offset.x() / 16f, -offset.y() / 16f, 0), null, null, null);
      transform = Transformation.compose(transform, Optional.of(itemOffset));
    }
    QuadCollection allQuads = quads.build();
    QuadCollection guiQuads = frontOnly(allQuads);
    return new VariantModels(
      new CuboidItemModelWrapper(List.of(), allQuads, properties, transform),
      new CuboidItemModelWrapper(List.of(), guiQuads, properties, transform));
  }

  /** One resolved material layer before legacy geometry generation. */
  private record ToolPartLayer(Material.Baked material, ExtraFaceData faceData) {}

  /**
   * Generates layers from top to bottom for pixel suppression, then restores
   * their original display order in the finished model.
   */
  private static void addToolPartLayers(ModelBaker baker, List<QuadCollection> reversedLayers,
                                        List<ToolPartLayer> layers, ItemLayerPixels usedPixels) {
    for (int i = layers.size() - 1; i >= 0; i--) {
      ToolPartLayer layer = layers.get(i);
      reversedLayers.add(MantleItemLayerGenerator.bake(
        baker, layer.material(), BlockModelRotation.IDENTITY, -1, layer.faceData(), usedPixels));
    }
  }

  /**
   * The legacy tool renderer exposed only the south/front face in GUI mode.
   * This is intentionally a post-bake filter so modifier, fluid, banner, and
   * trim overlays follow exactly the same rule as ordinary material layers.
   */
  private static QuadCollection frontOnly(QuadCollection source) {
    QuadCollection.Builder result = new QuadCollection.Builder();
    source.getAll().forEach(quad -> {
      if (quad.direction() == Direction.SOUTH) {
        result.addUnculledFace(quad);
      }
    });
    return result.build();
  }

  public record Part(String name, int index) {
    private static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("name").forGetter(Part::name),
      Codec.INT.optionalFieldOf("index", -1).forGetter(Part::index)
    ).apply(instance, Part::new));
  }

  /**
   * Describes a stored projectile rendered on top of a ranged tool.
   *
   * <p>This retains the legacy tool-model behavior: the value under {@code key}
   * is an encoded item stack, so tipped arrows and other compatible ammunition
   * use their own model and tint instead of a fixed replacement texture.</p>
   */
  public record AmmoDefinition(Identifier key, boolean flip, boolean left, Vector2fc offset,
                               Optional<Vector2fc> smallOffset, Optional<Vector2fc> largeOffset) {
    private static final Codec<AmmoDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Identifier.CODEC.fieldOf("key").forGetter(AmmoDefinition::key),
      Codec.BOOL.optionalFieldOf("flip", false).forGetter(AmmoDefinition::flip),
      Codec.BOOL.optionalFieldOf("left", false).forGetter(AmmoDefinition::left),
      ExtraCodecs.VECTOR2F.optionalFieldOf("offset", new Vector2f()).forGetter(AmmoDefinition::offset),
      ExtraCodecs.VECTOR2F.optionalFieldOf("small_offset").forGetter(AmmoDefinition::smallOffset),
      ExtraCodecs.VECTOR2F.optionalFieldOf("large_offset").forGetter(AmmoDefinition::largeOffset)
    ).apply(instance, AmmoDefinition::new));
  }

  /** Native model definition corresponding to {@code loader: tconstruct:tool}. */
  public record ToolUnbaked(Identifier parent, Map<String, Material> textures, Map<String, Material> brokenTextures,
                            List<Part> parts, Optional<List<Part>> brokenParts, boolean large, Vector2fc largeOffset,
                            List<Identifier> modifierMaps, Map<Identifier, NativeModifierModel> modifierModels,
                            List<NativeModifierModel> modifierConstants,
                            Optional<List<Identifier>> brokenModifierMaps,
                            Optional<Map<Identifier, NativeModifierModel>> brokenModifierModels,
                            Optional<List<NativeModifierModel>> brokenModifierConstants,
                            List<Identifier> firstModifiers, boolean showTraits,
                            Optional<AmmoDefinition> ammo) implements ItemModel.Unbaked {
    public static final MapCodec<ToolUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.optionalFieldOf("parent", DEFAULT_PARENT).forGetter(ToolUnbaked::parent),
      Codec.unboundedMap(Codec.STRING, Material.CODEC).optionalFieldOf("textures", Map.of()).forGetter(ToolUnbaked::textures),
      Codec.unboundedMap(Codec.STRING, Material.CODEC).optionalFieldOf("broken_textures", Map.of()).forGetter(ToolUnbaked::brokenTextures),
      Part.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(ToolUnbaked::parts),
      Part.CODEC.listOf().optionalFieldOf("broken_parts").forGetter(ToolUnbaked::brokenParts),
      Codec.BOOL.optionalFieldOf("large", false).forGetter(ToolUnbaked::large),
      ExtraCodecs.VECTOR2F.optionalFieldOf("large_offset", new Vector2f()).forGetter(ToolUnbaked::largeOffset),
      Identifier.CODEC.listOf().optionalFieldOf("modifier_maps", List.of()).forGetter(ToolUnbaked::modifierMaps),
      Codec.unboundedMap(Identifier.CODEC, NativeModifierModel.CODEC).optionalFieldOf("modifier_models", Map.of()).forGetter(ToolUnbaked::modifierModels),
      NativeModifierModel.CODEC.listOf().optionalFieldOf("modifier_constants", List.of()).forGetter(ToolUnbaked::modifierConstants),
      Identifier.CODEC.listOf().optionalFieldOf("broken_modifier_maps").forGetter(ToolUnbaked::brokenModifierMaps),
      Codec.unboundedMap(Identifier.CODEC, NativeModifierModel.CODEC).optionalFieldOf("broken_modifier_models").forGetter(ToolUnbaked::brokenModifierModels),
      NativeModifierModel.CODEC.listOf().optionalFieldOf("broken_modifier_constants").forGetter(ToolUnbaked::brokenModifierConstants),
      Identifier.CODEC.listOf().optionalFieldOf("first_modifiers", List.of()).forGetter(ToolUnbaked::firstModifiers),
      Codec.BOOL.optionalFieldOf("show_traits", false).forGetter(ToolUnbaked::showTraits),
      AmmoDefinition.CODEC.optionalFieldOf("ammo").forGetter(ToolUnbaked::ammo)
    ).apply(instance, ToolUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return new DynamicToolModel(this, context, transformation);
    }

    @Override
    public MapCodec<ToolUnbaked> type() {
      return MAP_CODEC;
    }
  }

  /** Native model definition corresponding to {@code loader: tconstruct:material}. */
  public record MaterialUnbaked(Identifier parent, Material texture, Vector2fc offset) implements ItemModel.Unbaked {
    public static final MapCodec<MaterialUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.optionalFieldOf("parent", DEFAULT_PARENT).forGetter(MaterialUnbaked::parent),
      Material.CODEC.fieldOf("texture").forGetter(MaterialUnbaked::texture),
      ExtraCodecs.VECTOR2F.optionalFieldOf("offset", new Vector2f()).forGetter(MaterialUnbaked::offset)
    ).apply(instance, MaterialUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return new DynamicMaterialModel(this, context, transformation);
    }

    @Override
    public MapCodec<MaterialUnbaked> type() {
      return MAP_CODEC;
    }
  }

  /** Native item model definition corresponding to {@code tconstruct:material_block}. */
  public record MaterialBlockUnbaked(Identifier model, List<String> slots) implements ItemModel.Unbaked {
    public static final MapCodec<MaterialBlockUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.fieldOf("model").forGetter(MaterialBlockUnbaked::model),
      Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(MaterialBlockUnbaked::slots)
    ).apply(instance, MaterialBlockUnbaked::new));

    public MaterialBlockUnbaked {
      slots = List.copyOf(slots);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return new DynamicMaterialBlockModel(this, context, transformation);
    }

    @Override
    public MapCodec<MaterialBlockUnbaked> type() {
      return MAP_CODEC;
    }
  }

  /** Native item model corresponding to the old {@code tconstruct:tank} geometry loader. */
  public record TankUnbaked(Identifier model, Vector3fc fluidFrom, Vector3fc fluidTo, int increments) implements ItemModel.Unbaked {
    public static final MapCodec<TankUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.fieldOf("model").forGetter(TankUnbaked::model),
      ExtraCodecs.VECTOR3F.fieldOf("fluid_from").forGetter(TankUnbaked::fluidFrom),
      ExtraCodecs.VECTOR3F.fieldOf("fluid_to").forGetter(TankUnbaked::fluidTo),
      Codec.intRange(1, Integer.MAX_VALUE).fieldOf("increments").forGetter(TankUnbaked::increments)
    ).apply(instance, TankUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return new DynamicTankModel(this, context, transformation);
    }

    @Override
    public MapCodec<TankUnbaked> type() {
      return MAP_CODEC;
    }
  }

  /** Runtime item model selecting a cached material or retextured block mesh. */
  private static final class DynamicMaterialBlockModel implements ItemModel {
    private final MaterialBlockUnbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private final ItemModel fallback;
    private final Map<MaterialBlockKey,ItemModel> cache = new ConcurrentHashMap<>();

    private DynamicMaterialBlockModel(MaterialBlockUnbaked definition, ItemModel.BakingContext context,
                                      Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
      this.fallback = bakeMaterialBlock(context, transformation, definition, Blocks.AIR, IMaterial.UNKNOWN_ID);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      Block texture = RetexturedHelper.getTexture(stack);
      MaterialVariantId material = texture == Blocks.AIR
                                   ? IMaterialItem.getMaterialFromStack(stack)
                                   : IMaterial.UNKNOWN_ID;
      if (texture == Blocks.AIR && IMaterial.UNKNOWN_ID.equals(material)) {
        fallback.update(output, stack, resolver, displayContext, level, owner, seed);
        return;
      }
      MaterialBlockKey key = new MaterialBlockKey(texture == Blocks.AIR ? null : texture, material);
      cache.computeIfAbsent(key, value -> bakeMaterialBlock(
        context, transformation, definition, value.texture(), value.material()))
        .update(output, stack, resolver, displayContext, level, owner, seed);
    }

    private record MaterialBlockKey(@Nullable Block texture, MaterialVariantId material) {}
  }

  /** Runtime item model selecting a cached tank mesh from the fluid component. */
  private static final class DynamicTankModel implements ItemModel {
    private final TankUnbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private final Map<FluidKey,ItemModel> cache = new ConcurrentHashMap<>();

    private DynamicTankModel(TankUnbaked definition, ItemModel.BakingContext context, Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      var tank = TankItem.getTank(stack, 1);
      FluidStack fluid = tank.getFluid();
      FluidKey key;
      if (fluid.isEmpty()) {
        key = FluidKey.EMPTY;
      } else {
        int capacity = Math.max(1, tank.getCapacity());
        int amount = Mth.clamp(fluid.getAmount() * definition.increments() / capacity, 1, definition.increments());
        key = new FluidKey(fluid.copy(), amount);
      }
      cache.computeIfAbsent(key, value -> bakeTank(context, transformation, definition, value))
        .update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }

  /**
   * Stable cache key for an inventory tank mesh.
   *
   * <p>{@link TankItem#getTank(ItemStack, int)} returns a fresh fluid stack on
   * every update.  Using the record-generated {@code FluidStack.equals}
   * therefore turned the old cache into an allocation-per-frame cache.  The
   * fluid model only depends on the fluid type/components and the quantized
   * fill amount, so compare those values explicitly and ignore the source
   * stack's raw amount.</p>
   */
  private static final class FluidKey {
    private static final FluidKey EMPTY = new FluidKey(FluidStack.EMPTY, 0);

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

  /** Bakes the base block mesh and, when present, the fluid cuboid for an item stack. */
  private static ItemModel bakeTank(ItemModel.BakingContext context, Matrix4fc transformation,
                                    TankUnbaked definition, FluidKey key) {
    ModelBaker baker = context.blockModelBaker();
    ResolvedModel resolved = baker.getModel(definition.model());
    TextureSlots textures = resolved.getTopTextureSlots();
    QuadCollection.Builder quads = new QuadCollection.Builder();
    quads.addAll(resolved.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY));

    Material.Baked particle = resolved.resolveParticleMaterial(textures, baker);
    if (!key.fluid().isEmpty()) {
      FluidStack fluid = key.fluid();
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      int color = fluidModel.fluidTintSource() == null
                  ? FluidTextureManager.getColor(fluid.getFluid().getFluidType())
                  : fluidModel.fluidTintSource().colorAsStack(fluid);
      int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);

      Vector3f from = new Vector3f(definition.fluidFrom());
      Vector3f to = new Vector3f(definition.fluidTo());
      if (fluid.getFluid().getFluidType().isLighterThanAir()) {
        from.y = to.y + key.amount() * (from.y - to.y) / definition.increments();
      } else {
        to.y = from.y + key.amount() * (to.y - from.y) / definition.increments();
      }

      Material still = new Material(
        fluidModel.stillMaterial().sprite().contents().name(),
        fluidModel.stillMaterial().forceTranslucent()
      );
      Material flowing = new Material(
        fluidModel.flowingMaterial().sprite().contents().name(),
        fluidModel.flowingMaterial().forceTranslucent()
      );
      TextureSlots.Data.Builder fluidTextures = new TextureSlots.Data.Builder()
        .addTexture("fluid", still)
        .addTexture("flowing_fluid", flowing);
      TextureSlots resolvedFluidTextures = new TextureSlots.Resolver()
        .addLast(fluidTextures.build())
        .resolve(resolved);
      ExtraFaceData faceData = new ExtraFaceData(color == -1 ? 0xFFFFFFFF : color, luminosity, true);
      CuboidModelElement element = fluidElement(from, to, faceData);
      QuadCollection fluidQuads = new UnbakedCuboidGeometry(java.util.List.of(element)).bake(
        resolvedFluidTextures, baker, BlockModelRotation.IDENTITY, resolved
      );
      quads.addAll(fluidQuads);
      particle = baker.materials().get(still, resolved);
    }

    ModelRenderProperties properties = new ModelRenderProperties(
      true, particle, resolved.getTopTransforms()
    );
    return new CuboidItemModelWrapper(java.util.List.of(), quads.build(), properties, transformation);
  }

  /** Default tank fluid faces match the legacy IncrementalFluidCuboid definition. */
  private static CuboidModelElement fluidElement(Vector3fc from, Vector3fc to, ExtraFaceData data) {
    Map<Direction,CuboidFace> faces = new java.util.EnumMap<>(Direction.class);
    for (Direction direction : Direction.values()) {
      faces.put(direction, new CuboidFace(
        null, -1, "fluid", fluidUvs(from, to, direction), com.mojang.math.Quadrant.R0,
        data, new MutableObject<>()
      ));
    }
    return new CuboidModelElement(from, to, faces, null, false, 0, data);
  }

  private static CuboidFace.UVs fluidUvs(Vector3fc from, Vector3fc to, Direction side) {
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
    return new CuboidFace.UVs(u1, v1, u2, v2);
  }

  private static final class DynamicToolModel implements ItemModel {
    private final ToolUnbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private final ModelRenderProperties parentProperties;
    private final Map<ToolKey, NativeTinkerItemModel> cache = new ConcurrentHashMap<>();

    private DynamicToolModel(ToolUnbaked definition, ItemModel.BakingContext context, Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
      ResolvedModel parent = context.blockModelBaker().getModel(definition.parent());
      this.parentProperties = ModelRenderProperties.fromResolvedModel(
        context.blockModelBaker(), parent, parent.getTopTextureSlots());
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      List<MaterialVariantId> materials = List.copyOf(MaterialIdNBT.from(item).getMaterials());
      boolean broken = !definition.brokenTextures().isEmpty() && ToolDamageUtil.isBroken(item);
      IToolStackView tool = ToolStack.from(item);
      // Components include modifier levels, dyed data, and fluid payloads.  A
      // component fingerprint keeps those dynamic overlays correct while the
      // actual mesh remains cached and immutable between updates.
      String dynamicKey = item.getComponents().toString();
      ToolKey key = new ToolKey(materials, broken, dynamicKey);
      Map<String, Material> textures = broken ? definition.brokenTextures() : definition.textures();
      List<Part> parts = broken ? definition.brokenParts().orElse(definition.parts()) : definition.parts();
      List<Identifier> modifierMapIds = broken
        ? definition.brokenModifierMaps().orElse(definition.modifierMaps())
        : definition.modifierMaps();
      NativeModifierModelMapManager.ModelMap sharedModels = NativeModifierModelMapManager.INSTANCE.get(modifierMapIds);
      Map<Identifier, NativeModifierModel> inlineModels = broken
        ? definition.brokenModifierModels().orElse(definition.modifierModels()) : definition.modifierModels();
      List<NativeModifierModel> inlineConstants = broken
        ? definition.brokenModifierConstants().orElse(definition.modifierConstants()) : definition.modifierConstants();
      Map<Identifier, NativeModifierModel> modifierModels = mergeModifierModels(sharedModels.modifiers(), inlineModels);
      List<NativeModifierModel> modifierConstants = mergeModifierConstants(sharedModels.constantModels(), inlineConstants);
      cache.computeIfAbsent(key, values -> bakeTool(context, transformation, definition.parent(), textures,
        parts, values.materials(), definition.large(), definition.largeOffset(),
        modifierModels, modifierConstants, definition.firstModifiers(), definition.showTraits(), tool))
        .update(output, item, resolver, displayContext, level, owner, seed);
      definition.ammo().ifPresent(ammo -> appendAmmo(
        output, resolver, displayContext, level, owner, seed, tool, ammo));
    }

    /** Appends the actual stored ammunition model using the tool's display transform. */
    private void appendAmmo(ItemStackRenderState output, ItemModelResolver resolver,
                            ItemDisplayContext displayContext, @Nullable ClientLevel level,
                            @Nullable ItemOwner owner, int seed, IToolStackView tool,
                            AmmoDefinition definition) {
      ClientLevel lookupLevel = level != null ? level : Minecraft.getInstance().level;
      if (lookupLevel == null || !tool.getPersistentData().contains(definition.key())) {
        return;
      }

      ItemStack ammo = ItemStackDataUtil.parse(
        lookupLevel.registryAccess(), tool.getPersistentData().getCompoundOrEmpty(definition.key()));
      if (ammo.isEmpty()) {
        return;
      }

      CapturingItemStackRenderState captured = new CapturingItemStackRenderState();
      resolver.updateForTopItem(captured, ammo, ItemDisplayContext.NONE, lookupLevel, owner, seed);
      if (captured.capturedLayers.isEmpty()) {
        return;
      }

      float flipOffset = definition.flip() ? 1 : 0;
      float z = definition.left() && displayContext.leftHand() ? -1f / 16 : 1f / 16;
      boolean large = this.definition.large() && usesLargeModel(displayContext);
      Vector2fc offset = this.definition.large()
        ? (large ? definition.largeOffset() : definition.smallOffset()).orElseGet(Vector2f::new)
        : definition.offset();
      float x = offset.x() / 16 + flipOffset;
      float y = -offset.y() / 16;
      if (large) {
        Vector2fc toolOffset = this.definition.largeOffset();
        x = (toolOffset.x() / 2 + offset.x() + 4) / 16 + flipOffset;
        y = (-toolOffset.y() / 2 - offset.y() + 4) / 16;
      }
      Transformation ammoTransform = new Transformation(
        new Vector3f(x, y, z + flipOffset),
        definition.flip() ? Axis.YP.rotationDegrees(-180) : null, null, null);
      Matrix4fc localTransform = Transformation.compose(transformation, Optional.of(ammoTransform));

      for (ItemStackRenderState.LayerRenderState source : captured.capturedLayers) {
        List<net.minecraft.client.resources.model.geometry.BakedQuad> quads = source.prepareQuadList();
        if (quads.isEmpty()) {
          continue;
        }
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.prepareQuadList().addAll(quads);
        layer.tintLayers().addAll(source.tintLayers());
        parentProperties.applyToLayer(layer, displayContext);
        layer.setLocalTransform(localTransform);
        layer.setExtents(() -> CuboidItemModelWrapper.computeExtents(quads));
      }
    }

    private record ToolKey(List<MaterialVariantId> materials, boolean broken, String dynamicKey) {}
  }

  private static Map<Identifier, NativeModifierModel> mergeModifierModels(
    Map<Identifier, NativeModifierModel> shared, Map<Identifier, NativeModifierModel> inline) {
    if (shared.isEmpty()) {
      return inline;
    }
    if (inline.isEmpty()) {
      return shared;
    }
    Map<Identifier, NativeModifierModel> merged = new HashMap<>(shared);
    merged.putAll(inline);
    return Map.copyOf(merged);
  }

  private static List<NativeModifierModel> mergeModifierConstants(
    List<NativeModifierModel> shared, List<NativeModifierModel> inline) {
    if (shared.isEmpty()) {
      return inline;
    }
    if (inline.isEmpty()) {
      return shared;
    }
    List<NativeModifierModel> merged = new ArrayList<>(shared.size() + inline.size());
    merged.addAll(shared);
    merged.addAll(inline);
    return List.copyOf(merged);
  }

  /** Exposes layers emitted by an arbitrary item model without reflecting into render state internals. */
  private static final class CapturingItemStackRenderState extends ItemStackRenderState {
    private final List<LayerRenderState> capturedLayers = new ArrayList<>();

    @Override
    public LayerRenderState newLayer() {
      LayerRenderState layer = super.newLayer();
      capturedLayers.add(layer);
      return layer;
    }
  }

  private static final class DynamicMaterialModel implements ItemModel {
    private final MaterialUnbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private final Map<MaterialVariantId, NativeTinkerItemModel> cache = new ConcurrentHashMap<>();

    private DynamicMaterialModel(MaterialUnbaked definition, ItemModel.BakingContext context, Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      MaterialVariantId material = IMaterialItem.getMaterialFromStack(item);
      cache.computeIfAbsent(material, ignored -> bakeMaterial(context, transformation, definition.parent(), definition.texture(), material, definition.offset()))
        .update(output, item, resolver, displayContext, level, owner, seed);
    }
  }

}

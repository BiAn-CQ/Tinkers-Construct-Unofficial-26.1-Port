package slimeknights.tconstruct.plugin.jsonthings;

import dev.gigaherz.jsonthings.things.IFlexBlock;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.IBlockSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.util.Lazy;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.plugin.jsonthings.block.FlexBurningLiquidBlock;
import slimeknights.tconstruct.plugin.jsonthings.block.FlexMobEffectLiquidBlock;

import java.util.Objects;

/** Collection of custom Json Things block types added by Tinkers. */
public final class FlexBlockTypes {
  private FlexBlockTypes() {}

  public static void init() {
    register("burning_liquid", data -> {
      Identifier fluidName = Loadables.RESOURCE_LOCATION.getOrDefault(data, "fluid", null);
      int burnTime = GsonHelper.getAsInt(data, "burn_time");
      float damage = GsonHelper.getAsFloat(data, "damage");
      return (properties, builder) -> new FlexBurningLiquidBlock(
        properties.liquid(), builder,
        flowingFluid(Objects.requireNonNullElse(fluidName, builder.getRegistryName())),
        burnTime, damage);
    });

    register("mob_effect_liquid", data -> {
      Identifier fluidName = Loadables.RESOURCE_LOCATION.getOrDefault(data, "fluid", null);
      Identifier effectName = Loadables.RESOURCE_LOCATION.getIfPresent(data, "effect");
      int effectLevel = GsonHelper.getAsInt(data, "effect_level", 1);
      return (properties, builder) -> {
        Lazy<MobEffect> effect = Lazy.of(() -> Loadables.MOB_EFFECT.fromKey(effectName, "effect"));
        return new FlexMobEffectLiquidBlock(
          properties.liquid(), builder,
          flowingFluid(Objects.requireNonNullElse(fluidName, builder.getRegistryName())),
          () -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get()),
            5 * 20, effectLevel - 1));
      };
    });
  }

  private static FlowingFluid flowingFluid(Identifier name) {
    if (Loadables.FLUID.fromKey(name, "fluid") instanceof FlowingFluid flowing) {
      return flowing;
    }
    throw new IllegalStateException("Liquid block requires a flowing fluid: " + name);
  }

  private static <T extends Block & IFlexBlock> void register(String name, IBlockSerializer<T> serializer) {
    FlexBlockType.register(TConstruct.resourceString(name), serializer,
      FlexBlockType.DefaultTypeProperties.builder()
        .defaultSeeThrough(true)
        .stockProperties(LiquidBlock.LEVEL));
  }
}

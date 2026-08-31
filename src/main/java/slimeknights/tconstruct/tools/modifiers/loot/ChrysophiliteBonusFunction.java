package slimeknights.tconstruct.tools.modifiers.loot;

import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.shared.TinkerAttributes;

import java.util.List;
import java.util.Set;

/** Loot modifier to boost drops based on teh chrysophilite amount */
public class ChrysophiliteBonusFunction extends LootItemConditionalFunction {
  public static final MapCodec<ChrysophiliteBonusFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
    BonusFormula.CODEC.forGetter(function -> function.formula),
    com.mojang.serialization.Codec.BOOL.optionalFieldOf("include_base", true).forGetter(function -> function.includeBase)
  )).apply(instance, ChrysophiliteBonusFunction::new));

  /** Formula to apply */
  private final BonusFormula formula;
  /** If true, the includes the helmet in the level, if false level is just gold pieces */
  private final boolean includeBase;
  protected ChrysophiliteBonusFunction(List<LootItemCondition> conditions, BonusFormula formula, boolean includeBase) {
    super(conditions);
    this.formula = formula;
    this.includeBase = includeBase;
  }

  /** Creates a generic builder */
  public static Builder<?> builder(BonusFormula formula, boolean includeBase) {
    return simpleBuilder(conditions -> new ChrysophiliteBonusFunction(conditions, formula, includeBase));
  }

  /** Creates a builder for the binomial with bonus formula */
  public static Builder<?> binomialWithBonusCount(float probability, int extra, boolean includeBase) {
    return builder(new BonusFormula.BinomialWithBonusCount(extra, probability), includeBase);
  }

  /** Creates a builder for the ore drops formula */
  public static Builder<?> oreDrops(boolean includeBase) {
    return builder(new BonusFormula.OreDrops(), includeBase);
  }

  /** Creates a builder for the uniform bonus count */
  public static Builder<?> uniformBonusCount(int bonusMultiplier, boolean includeBase) {
    return builder(new BonusFormula.UniformBonusCount(bonusMultiplier), includeBase);
  }

  protected ItemStack run(ItemStack stack, LootContext context) {
    if (context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
      int level = (int) entity.getAttributeValue(TinkerAttributes.CHRYSOPHILITE);
      if (!includeBase) {
        level--;
      }
      if (level > 0) {
        stack.setCount(formula.calculateNewCount(context.getRandom(), stack.getCount(), level));
      }
    }
    return stack;
  }

  @Override
  public Set<ContextKey<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public MapCodec<? extends LootItemConditionalFunction> codec() {
    return CODEC;
  }
}

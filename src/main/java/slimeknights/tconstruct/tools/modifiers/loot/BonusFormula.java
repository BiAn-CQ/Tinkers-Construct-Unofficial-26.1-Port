package slimeknights.tconstruct.tools.modifiers.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.function.Function;

/** Formula used by modifier-driven bonus loot functions. Mirrors vanilla's three apply-bonus formulas. */
public interface BonusFormula {
  FormulaType<BinomialWithBonusCount> BINOMIAL = new FormulaType<>(
    Identifier.withDefaultNamespace("binomial_with_bonus_count"), BinomialWithBonusCount.CODEC);
  FormulaType<OreDrops> ORE_DROPS = new FormulaType<>(
    Identifier.withDefaultNamespace("ore_drops"), OreDrops.CODEC);
  FormulaType<UniformBonusCount> UNIFORM = new FormulaType<>(
    Identifier.withDefaultNamespace("uniform_bonus_count"), UniformBonusCount.CODEC);
  Map<Identifier,FormulaType<?>> TYPES = Map.of(
    BINOMIAL.id(), BINOMIAL,
    ORE_DROPS.id(), ORE_DROPS,
    UNIFORM.id(), UNIFORM);
  Codec<FormulaType<?>> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
    id -> {
      FormulaType<?> type = TYPES.get(id);
      return type == null ? DataResult.error(() -> "No bonus formula type with id: '" + id + "'") : DataResult.success(type);
    }, FormulaType::id);
  MapCodec<BonusFormula> CODEC = ExtraCodecs.dispatchOptionalValue(
    "formula", "parameters", TYPE_CODEC, BonusFormula::type, FormulaType::codec);

  int calculateNewCount(RandomSource random, int originalCount, int level);

  FormulaType<?> type();

  record FormulaType<T extends BonusFormula>(Identifier id, Codec<T> codec) {}

  record BinomialWithBonusCount(int extraRounds, float probability) implements BonusFormula {
    static final Codec<BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("extra").forGetter(BinomialWithBonusCount::extraRounds),
      Codec.FLOAT.fieldOf("probability").forGetter(BinomialWithBonusCount::probability)
    ).apply(instance, BinomialWithBonusCount::new));

    @Override
    public int calculateNewCount(RandomSource random, int originalCount, int level) {
      for (int i = 0; i < level + extraRounds; i++) {
        if (random.nextFloat() < probability) {
          originalCount++;
        }
      }
      return originalCount;
    }

    @Override
    public FormulaType<?> type() {
      return BINOMIAL;
    }
  }

  record OreDrops() implements BonusFormula {
    static final Codec<OreDrops> CODEC = MapCodec.unit(new OreDrops()).codec();

    @Override
    public int calculateNewCount(RandomSource random, int originalCount, int level) {
      if (level <= 0) {
        return originalCount;
      }
      int multiplier = Math.max(0, random.nextInt(level + 2) - 1);
      return originalCount * (multiplier + 1);
    }

    @Override
    public FormulaType<?> type() {
      return ORE_DROPS;
    }
  }

  record UniformBonusCount(int bonusMultiplier) implements BonusFormula {
    static final Codec<UniformBonusCount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("bonusMultiplier").forGetter(UniformBonusCount::bonusMultiplier)
    ).apply(instance, UniformBonusCount::new));

    @Override
    public int calculateNewCount(RandomSource random, int originalCount, int level) {
      return originalCount + random.nextInt(bonusMultiplier * level + 1);
    }

    @Override
    public FormulaType<?> type() {
      return UNIFORM;
    }
  }
}

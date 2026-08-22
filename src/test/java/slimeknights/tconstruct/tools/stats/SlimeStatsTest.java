package slimeknights.tconstruct.tools.stats;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.modules.capacity.OverslimeModule;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;

class SlimeStatsTest extends CoreTestBootstrap {
  @Test
  void appliesDurabilityAndOverslimeIndependently() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();

    new SlimeStats(200, 75).apply(builder, 1.5f);

    StatsNBT stats = builder.build();
    assertThat(stats.get(ToolStats.DURABILITY)).isEqualTo(300f);
    assertThat(stats.get(OverslimeModule.OVERSLIME_STAT)).isEqualTo(112.5f);
  }
}

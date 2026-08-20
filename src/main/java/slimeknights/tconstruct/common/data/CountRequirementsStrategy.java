package slimeknights.tconstruct.common.data;

import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRequirements.Strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CountRequirementsStrategy implements Strategy {
  private final int[] sizes;
  public CountRequirementsStrategy(int... sizes) {
    this.sizes = sizes;
  }

  @Override
  public AdvancementRequirements create(Collection<String> strings) {
    List<List<String>> requirements = new ArrayList<>(sizes.length);
    List<String> criteria = new ArrayList<>(strings);
    int nextIndex = 0;
    for (int size : sizes) {
      requirements.add(new ArrayList<>(criteria.subList(nextIndex, nextIndex + size)));
      nextIndex += size;
    }
    return new AdvancementRequirements(requirements);
  }
}

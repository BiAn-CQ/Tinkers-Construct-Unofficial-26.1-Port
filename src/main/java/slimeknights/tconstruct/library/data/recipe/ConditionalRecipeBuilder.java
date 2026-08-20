package slimeknights.tconstruct.library.data.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;

import java.util.ArrayList;
import java.util.List;

/** Builds ordered conditional variants using NeoForge's native RecipeOutput conditions. */
public final class ConditionalRecipeBuilder {
  private final List<Branch> branches = new ArrayList<>();
  private final List<ICondition> pendingConditions = new ArrayList<>();

  private ConditionalRecipeBuilder() {}

  public static ConditionalRecipeBuilder builder() {
    return new ConditionalRecipeBuilder();
  }

  public ConditionalRecipeBuilder addCondition(ICondition condition) {
    pendingConditions.add(condition);
    return this;
  }

  public ConditionalRecipeBuilder addRecipe(RecipeGenerator generator) {
    branches.add(new Branch(List.copyOf(pendingConditions), generator));
    pendingConditions.clear();
    return this;
  }

  /** Branch builders already forward their native advancement holders. */
  public ConditionalRecipeBuilder generateAdvancement() {
    return this;
  }

  public void build(RecipeOutput output, Identifier id) {
    if (!pendingConditions.isEmpty()) {
      throw new IllegalStateException("Conditional recipe " + id + " has conditions without a recipe");
    }
    List<ICondition> previousBranches = new ArrayList<>();
    for (int index = 0; index < branches.size(); index++) {
      Branch branch = branches.get(index);
      List<ICondition> conditions = new ArrayList<>(branch.conditions);
      if (!previousBranches.isEmpty()) {
        conditions.add(new NotCondition(previousBranches.size() == 1 ? previousBranches.getFirst() : new OrCondition(previousBranches)));
      }
      Identifier branchId = index == 0 ? id : id.withSuffix("_fallback_" + index);
      branch.generator.save(output.withConditions(conditions.toArray(ICondition[]::new)), branchId);
      previousBranches.add(branch.conditions.size() == 1 ? branch.conditions.getFirst() : new AndCondition(branch.conditions));
    }
  }

  @FunctionalInterface
  public interface RecipeGenerator {
    void save(RecipeOutput output, Identifier id);
  }

  private record Branch(List<ICondition> conditions, RecipeGenerator generator) {}
}

package slimeknights.tconstruct.library.modifiers.modules.combat;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.json.RandomLevelingValue;
import slimeknights.tconstruct.library.utils.EffectCureUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MobEffectModuleTest {
  @BeforeAll static void bindItemComponents() {
    for (var item : List.of(Items.MILK_BUCKET, Items.HONEY_BOTTLE)) {
      if (!item.builtInRegistryHolder().areComponentsBound()) {
        item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
      }
    }
  }

  private static MobEffectInstance apply(List<Item> cures) {
    var target = mock(LivingEntity.class);
    var cause = mock(Entity.class);
    var effect = new MobEffectModule.ModifierMobEffect(MobEffects.SPEED.value(),
      RandomLevelingValue.flat(2), RandomLevelingValue.flat(100), LivingEntityPredicate.ANY, cures);
    effect.applyEffect(target, 1, cause);
    var capture = ArgumentCaptor.forClass(MobEffectInstance.class);
    verify(target).addEffect(capture.capture(), same(cause));
    var instance = capture.getValue();
    assertThat(instance.getDuration()).isEqualTo(100);
    assertThat(instance.getAmplifier()).isEqualTo(1);
    return instance;
  }

  @Test void explicitCureItemsBelongToTheAppliedInstance() {
    var instance = apply(List.of(Items.HONEY_BOTTLE));
    assertThat(EffectCureUtil.canCure(instance, new ItemStack(Items.HONEY_BOTTLE))).isTrue();
    assertThat(EffectCureUtil.canCure(instance, new ItemStack(Items.MILK_BUCKET))).isFalse();
  }

  @Test void emptyCureListDisablesCures() {
    assertThat(EffectCureUtil.hasCures(apply(List.of()))).isFalse();
  }

  @Test void absentCureListKeepsDefaultMilkCure() {
    assertThat(EffectCureUtil.canCure(apply(null), new ItemStack(Items.MILK_BUCKET))).isTrue();
  }
}

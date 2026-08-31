package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.armor.EffectImmunityModule;
import slimeknights.tconstruct.library.tools.capability.TinkerAttachments;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DEFAULT;
import static net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectImmunityTest {
  @Test
  void shellGutBlocksPoisonOneHungerTwoAndNauseaOne() {
    Fixture fixture = new Fixture();
    fixture.equipShellGut();
    assertThat(fixture.result(MobEffects.POISON, 0)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.HUNGER, 0)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.HUNGER, 1)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.NAUSEA, 0)).isEqualTo(DO_NOT_APPLY);
  }

  @Test
  void strongerAndUnrelatedEffectsAreNotBlocked() {
    Fixture fixture = new Fixture();
    fixture.equipShellGut();
    assertThat(fixture.result(MobEffects.POISON, 1)).isEqualTo(DEFAULT);
    assertThat(fixture.result(MobEffects.HUNGER, 2)).isEqualTo(DEFAULT);
    assertThat(fixture.result(MobEffects.NAUSEA, 1)).isEqualTo(DEFAULT);
    assertThat(fixture.result(MobEffects.SLOWNESS, 0)).isEqualTo(DEFAULT);
  }

  @Test
  void effectiveLevelsAndUnequippingAdjustImmunity() {
    Fixture fixture = new Fixture();
    EffectImmunityModule module = new EffectImmunityModule(MobEffects.POISON, LevelingInt.LEVEL);
    when(fixture.modifier.getEffectiveLevel()).thenReturn(2f);
    fixture.equip(module);
    assertThat(fixture.result(MobEffects.POISON, 1)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.POISON, 2)).isEqualTo(DEFAULT);
    module.onUnequip(fixture.tool, fixture.modifier, fixture.context);
    assertThat(fixture.result(MobEffects.POISON, 0)).isEqualTo(DEFAULT);
  }

  @Test
  void brokenArmorDoesNotGrantImmunity() {
    Fixture fixture = new Fixture();
    when(fixture.tool.isBroken()).thenReturn(true);
    fixture.equipShellGut();
    assertThat(fixture.result(MobEffects.POISON, 0)).isEqualTo(DEFAULT);
  }

  @Test
  void sharedImmunityModulesAlsoMatchRegisteredEffects() {
    Fixture fixture = new Fixture();
    fixture.equip(new EffectImmunityModule(MobEffects.SLOWNESS, LevelingInt.LEVEL));
    fixture.equip(new EffectImmunityModule(MobEffects.WITHER, LevelingInt.LEVEL));
    fixture.equip(new EffectImmunityModule(MobEffects.POISON));
    assertThat(fixture.result(MobEffects.SLOWNESS, 0)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.WITHER, 0)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.POISON, 10)).isEqualTo(DO_NOT_APPLY);
    assertThat(fixture.result(MobEffects.WITHER, 1)).isEqualTo(DEFAULT);
  }

  private static class Fixture {
    private final LivingEntity entity = mock(LivingEntity.class);
    private final IToolStackView tool = mock(IToolStackView.class);
    private final ModifierEntry modifier = mock(ModifierEntry.class);
    private final EquipmentChangeContext context = mock(EquipmentChangeContext.class);

    private Fixture() {
      TinkerDataCapability.Holder data = new TinkerDataCapability.Holder();
      when(entity.getData(TinkerAttachments.TINKER_DATA)).thenReturn(data);
      when(context.getDataHolder()).thenReturn(data);
      when(context.getChangedSlot()).thenReturn(EquipmentSlot.LEGS);
      when(modifier.getLevel()).thenReturn(1);
      when(modifier.getEffectiveLevel()).thenReturn(1f);
    }

    private void equip(EffectImmunityModule module) {
      module.onEquip(tool, modifier, context);
    }

    private void equipShellGut() {
      equip(new EffectImmunityModule(MobEffects.POISON, LevelingInt.LEVEL));
      equip(new EffectImmunityModule(MobEffects.HUNGER, new LevelingInt(1, 1)));
      equip(new EffectImmunityModule(MobEffects.NAUSEA, LevelingInt.LEVEL));
    }

    private MobEffectEvent.Applicable.Result result(Holder<MobEffect> effect, int amplifier) {
      MobEffectEvent.Applicable event = new MobEffectEvent.Applicable(entity, new MobEffectInstance(effect, 600, amplifier), null);
      ModifierEvents.isPotionApplicable(event);
      return event.getResult();
    }
  }
}

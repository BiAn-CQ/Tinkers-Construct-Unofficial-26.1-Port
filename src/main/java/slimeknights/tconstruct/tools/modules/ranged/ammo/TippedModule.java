package slimeknights.tconstruct.tools.modules.ranged.ammo;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.utils.RomanNumeralHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/** Module allowing arrows to be tipped, applying their effect to the target */
public enum TippedModule implements ModifierModule, ProjectileLaunchModifierHook.NoShooter, ProjectileHitModifierHook, ModifierRemovalHook, DisplayNameModifierHook, TooltipModifierHook {
  INSTANCE;

  private static final String FORMAT = TConstruct.makeTranslationKey("modifier", "tipped.format");
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<TippedModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_SHOT, ModifierHooks.PROJECTILE_THROWN, ModifierHooks.PROJECTILE_HIT, ModifierHooks.DISPLAY_NAME, ModifierHooks.TOOLTIP, ModifierHooks.REMOVE);
  public static final RecordLoadable<TippedModule> LOADER = new SingletonLoader<>(INSTANCE);

  @Override
  public RecordLoadable<TippedModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }


  /* Data */

  @Override
  public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity shooter, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    Identifier key = modifier.getId().location();
    IModDataView toolData = tool.getPersistentData();
    if (toolData.contains(key)) {
      persistentData.putString(key, toolData.getStringOr(key, ""));
    }
  }

  @Nullable
  @Override
  public Component onRemoved(IToolStackView tool, Modifier modifier) {
    tool.getPersistentData().remove(modifier.getId());
    return null;
  }


  /* Effects */

  /** Gets the divisor for the duration */
  private static int getDivisor(ModifierEntry modifier) {
    return 1 << Math.max(4 - modifier.intEffectiveLevel(), 0);
  }

  @Override
  public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
    Identifier key = modifier.getId().location();
    if (target != null && persistentData.contains(key)) {
      Identifier id = Identifier.tryParse(persistentData.getStringOr(key, ""));
      if (id != null) {
        Entity source = projectile.getEffectSource();
        int divisor = getDivisor(modifier);
        int oldHurtTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        Holder.Reference<Potion> potion = BuiltInRegistries.POTION.get(id).orElse(null);
        if (potion == null) {
          return false;
        }
        for (MobEffectInstance instance : potion.value().getEffects()) {
          MobEffect effect = instance.getEffect().value();
          if (effect.isInstantenous()) {
            if (projectile.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
              effect.applyInstantenousEffect(serverLevel, source, projectile.getOwner(), target, instance.getAmplifier(), 1f / (divisor * 0.75f));
            }
          } else {
            target.addEffect(new MobEffectInstance(instance.getEffect(), Math.max(instance.mapDuration(i -> i / divisor), 1), instance.getAmplifier(), instance.isAmbient(), instance.isVisible()), source);
          }
        }
        target.invulnerableTime = oldHurtTime;
      }
    }
    return false;
  }


  /* Display */

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    Identifier key = modifier.getId().location();
    IModDataView toolData = tool.getPersistentData();
    if (toolData.contains(key)) {
      Identifier id = Identifier.tryParse(toolData.getStringOr(key, ""));
      if (id != null) {
        BuiltInRegistries.POTION.get(id).ifPresent(potion ->
          PotionContents.addPotionTooltip(potion.value().getEffects(), tooltip::add, 1f / getDivisor(modifier), 20.0f));
      }
    }
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    Identifier key = entry.getId().location();
    IModDataView toolData = tool.getPersistentData();
    if (toolData.contains(key)) {
      Identifier id = Identifier.tryParse(toolData.getStringOr(key, ""));
      if (id != null) {
        Holder.Reference<Potion> potion = BuiltInRegistries.POTION.get(id).orElse(null);
        if (potion != null) {
          // formats as Tipped <level> (<potion>)
          return Component.translatable(FORMAT,
            RomanNumeralHelper.getNumeral(entry.getLevel()),
            new PotionContents(Optional.of(potion), Optional.empty(), List.of(), Optional.empty()).getName("item.minecraft.potion.effect.")
          ).withStyle(style -> style.withColor(PotionContents.getColorOptional(potion.value().getEffects()).orElse(0x385DC6)));
        }
      }
    }
    return name;
  }
}

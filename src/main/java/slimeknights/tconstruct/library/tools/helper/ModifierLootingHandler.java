package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.shared.TinkerEffects;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Integrates Tinkers looting hooks with the native 1.21 enchantment-driven loot pipeline. */
public class ModifierLootingHandler {
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static final Map<UUID,RestoredStack> RESTORE_STACKS = new HashMap<>();
  private static boolean init = false;

  public static void init() {
    if (init) {
      return;
    }
    init = true;
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, ModifierLootingHandler::beforeLoot);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDropsEvent.class, ModifierLootingHandler::afterLoot);
    NeoForge.EVENT_BUS.addListener(ModifierLootingHandler::onLeaveServer);
  }

  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /** Gets the effective vanilla looting level represented by an entity loot context. */
  public static int getLootingLevel(LootContext context) {
    Entity attacker = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
    if (attacker instanceof LivingEntity living) {
      return EnchantmentHelper.getEnchantmentLevel(RegistryHelper.getHolder(living.registryAccess(), Enchantments.LOOTING), living);
    }
    return 0;
  }

  private static void beforeLoot(LivingDeathEvent event) {
    if (event.isCanceled()) {
      return;
    }
    LivingEntity target = event.getEntity();
    DamageSource damageSource = event.getSource();
    if (damageSource.getEntity() instanceof LivingEntity holder) {
      var looting = RegistryHelper.getHolder(holder.registryAccess(), Enchantments.LOOTING);
      int vanillaLevel = EnchantmentHelper.getEnchantmentLevel(looting, holder);
      int level = getLootingLevel(target, damageSource, holder, vanillaLevel);
      if (level != vanillaLevel) {
        ItemStack original = holder.getMainHandItem();
        ItemStack replacement = original.copy();
        if (replacement.isEmpty()) {
          replacement = new ItemStack(Items.WOODEN_SWORD);
        }
        replacement.enchant(looting, level);
        RESTORE_STACKS.put(target.getUUID(), new RestoredStack(holder, original));
        holder.setItemSlot(EquipmentSlot.MAINHAND, replacement);
      }
    }
  }

  private static void afterLoot(LivingDropsEvent event) {
    RestoredStack restore = RESTORE_STACKS.remove(event.getEntity().getUUID());
    if (restore != null) {
      restore.holder.setItemSlot(EquipmentSlot.MAINHAND, restore.stack);
    }
  }

  /** Computes the effective level using the same weapon, projectile and armor hooks as 1.20.1. */
  public static int getLootingLevel(LivingEntity target, DamageSource damageSource, LivingEntity holder, int vanillaLevel) {
    if (damageSource.is(TinkerDamageTypes.BLEEDING)) {
      return Math.max(0, TinkerEffect.getAmplifier(target, TinkerEffects.bleeding.get()));
    }

    Entity direct = damageSource.getDirectEntity();
    int level = vanillaLevel;
    LootingContext context;
    IToolStackView tool = null;
    if (direct instanceof Projectile) {
      ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(direct);
      context = new LootingContext(holder, target, damageSource, null);
      if (!modifiers.isEmpty()) {
        ModDataNBT persistentData = PersistentDataCapability.getOrWarn(direct);
        level = LootingModifierHook.getLooting(new DummyToolStack(Items.AIR, modifiers, persistentData), context, 0);
      }
    } else {
      EquipmentSlot slotType = getLootingSlot(holder);
      context = new LootingContext(holder, target, damageSource, slotType);
      ItemStack held = holder.getItemBySlot(slotType);
      if (held.is(TinkerTags.Items.MODIFIABLE)) {
        tool = ToolStack.from(held);
        level = LootingModifierHook.getLooting(tool, context, level);
      } else if (slotType != EquipmentSlot.MAINHAND) {
        level = 0;
      }
    }
    level = ArmorLootingModifierHook.getLooting(tool, context, level);
    return Math.max(level, 0);
  }

  private static void onLeaveServer(PlayerLoggedOutEvent event) {
    LOOTING_OFFHAND.remove(event.getEntity().getUUID());
  }

  private record RestoredStack(LivingEntity holder, ItemStack stack) {}
}

package slimeknights.tconstruct.library.tools.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.ItemAbility;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;

public class ModifiableArmorItem extends Item implements IModifiableDisplay {
  /** Volatile modifier tag to make piglins neutal when worn */
  public static final Identifier PIGLIN_NEUTRAL = TConstruct.getResource("piglin_neutral");
  /** Volatile modifier tag to make this item an elytra */
  public static final Identifier ELYTRA = TConstruct.getResource("elyta");
  /** Volatile flag for a boot item to walk on powdered snow. Cold immunity is handled through a tag */
  public static final Identifier SNOW_BOOTS = TConstruct.getResource("snow_boots");
  /** Volatile flag for an item to act as an enderman mask, stopping them from getting angry. */
  public static final Identifier ENDERMASK = TConstruct.getResource("endermask");

  @Getter
  private final ToolDefinition toolDefinition;
  public ToolDefinition getToolDefinition() { return toolDefinition; }
  private final ArmorSlotType armorSlotType;
  /** Cache of the tool built for rendering */
  private ItemStack toolForRendering = null;
  public ModifiableArmorItem(Holder<ArmorMaterial> materialIn, ArmorSlotType armorSlotType, Properties builderIn, ToolDefinition toolDefinition) {
    this(materialIn, armorSlotType, builderIn, toolDefinition, materialIn.value().assetId());
  }

  public ModifiableArmorItem(Holder<ArmorMaterial> materialIn, ArmorSlotType armorSlotType, Properties builderIn,
                             ToolDefinition toolDefinition, ResourceKey<EquipmentAsset> asset) {
    super(builderIn.component(DataComponents.EQUIPPABLE, Equippable.builder(armorSlotType.getEquipmentSlot())
      .setEquipSound(materialIn.value().equipSound())
      .setAsset(asset)
      .build()));
    this.armorSlotType = armorSlotType;
    this.toolDefinition = toolDefinition;
  }

  public ModifiableArmorItem(ModifiableArmorMaterial material, ArmorSlotType armorSlotType, Properties properties) {
    this(material.asArmorMaterial(), armorSlotType, properties, Objects.requireNonNull(material.getArmorDefinition(armorSlotType), "Missing tool definition for " + armorSlotType.getName()));
  }

  public ArmorSlotType getArmorSlotType() {
    return armorSlotType;
  }

  public EquipmentSlot getEquipmentSlot() {
    return armorSlotType.getEquipmentSlot();
  }

  /* Basic properties */

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return 1;
  }

  public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
    return ModifierUtil.checkVolatileFlag(stack, PIGLIN_NEUTRAL);
  }

  public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
    return armorSlotType == ArmorSlotType.BOOTS && ModifierUtil.checkVolatileFlag(stack, SNOW_BOOTS);
  }

  public boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {
    return armorSlotType == ArmorSlotType.HELMET && ModifierUtil.checkVolatileFlag(stack, ENDERMASK);
  }

  @Override
  public boolean canPerformAction(ItemInstance stack, ItemAbility toolAction) {
    return stack instanceof ItemStack itemStack && ModifierUtil.canPerformAction(ToolStack.from(itemStack), toolAction);
  }

  @Override
  public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
    return true;
  }


  /* Enchantments */

  public boolean isEnchantable(ItemStack stack) {
    return false;
  }

  public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
    return false;
  }

  @Override
  public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
    return enchantment.is(EnchantmentTags.CURSE);
  }

  @Override
  public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
    return EnchantmentModifierHook.getEnchantmentLevel(stack, enchantment);
  }

  @Override
  public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
    return EnchantmentModifierHook.getAllEnchantments(stack);
  }


  /* Loading */

  public void verifyComponentsAfterLoad(ItemStack stack) {
    CompoundTag nbt = slimeknights.tconstruct.library.utils.ItemStackDataUtil.getTag(stack);
    if (nbt != null) {
      ToolStack.verifyTag(this, nbt, getToolDefinition());
      slimeknights.tconstruct.library.utils.ItemStackDataUtil.setTag(stack, nbt);
      updateDynamicComponents(stack);
    }
  }

  @Override
  public void onCraftedBy(ItemStack stack, Player playerIn) {
    ToolStack.ensureInitialized(stack, getToolDefinition());
    super.onCraftedBy(stack, playerIn);
  }

  @Override
  public InteractionResult use(Level levelIn, Player playerIn, InteractionHand handIn) {
    if (playerIn.isCrouching()) {
      ItemStack stack = playerIn.getItemInHand(handIn);
      InteractionResult result = ToolInventoryCapability.tryOpenContainer(stack, null, getToolDefinition(), playerIn, Util.getSlotType(handIn));
      if (result.consumesAction()) {
        return result;
      }
    }
    return super.use(levelIn, playerIn, handIn);
  }


  /* Display */

  @Override
  public boolean isFoil(ItemStack stack) {
    // we use enchantments to handle some modifiers, so don't glow from them
    // however, if a modifier wants to glow let them
    return ModifierUtil.checkVolatileFlag(stack, SHINY);
  }

  public Rarity getRarity(ItemStack stack) {
    return RarityModule.getRarity(stack);
  }


  /* Item entity */

  @Override
  public boolean hasCustomEntity(ItemStack stack) {
    return IndestructibleItemEntity.hasCustomEntity(stack);
  }

  @Nullable
  @Override
  public Entity createEntity(Level level, Entity original, ItemStack stack) {
    return IndestructibleItemEntity.createFrom(level, original, stack);
  }

  @Override
  public void onDestroyed(ItemEntity entity) {
    ToolInventoryCapability.onDestroyed(entity);
  }


  /* Damage/Durability */

  public boolean isRepairable(ItemStack stack) {
    // handle in the tinker station
    return false;
  }

  public boolean canBeDepleted() {
    return true;
  }

  @Override
  public int getMaxDamage(ItemStack stack) {
    return ToolDamageUtil.getFakeMaxDamage(stack);
  }

  @Override
  public int getDamage(ItemStack stack) {
    if (!canBeDepleted()) {
      return 0;
    }
    return ToolStack.from(stack).getDamage();
  }

  @Override
  public void setDamage(ItemStack stack, int damage) {
    if (canBeDepleted()) {
      ToolStack.from(stack).setDamage(damage);
    }
  }

  @Override
  public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T damager, Consumer<Item> onBroken) {
    // We basically emulate Itemstack.damageItem here. We always return 0 to skip the handling in ItemStack.
    // If we don't tools ignore our damage logic
    if (canBeDepleted() && ToolDamageUtil.damage(ToolStack.from(stack), amount, damager, stack)) {
      onBroken.accept(stack.getItem());
    }

    return 0;
  }


  /* Durability display */

  @Override
  public boolean isBarVisible(ItemStack pStack) {
    return DurabilityDisplayModifierHook.showDurabilityBar(pStack);
  }

  @Override
  public int getBarColor(ItemStack pStack) {
    return DurabilityDisplayModifierHook.getDurabilityRGB(pStack);
  }

  @Override
  public int getBarWidth(ItemStack pStack) {
    return DurabilityDisplayModifierHook.getDurabilityWidth(pStack);
  }


  /* Armor properties */

  public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
    return false;
  }


  @Override
  public Multimap<Attribute,AttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
    if (slot != getEquipmentSlot()) {
      return ImmutableMultimap.of();
    }

    ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
    if (!tool.isBroken()) {
      // base stats
      StatsNBT statsNBT = tool.getStats();
      Identifier attributeId = Identifier.withDefaultNamespace("armor." + armorSlotType.getName());
      float armor = statsNBT.get(ToolStats.ARMOR);
      if (armor > 0) {
        builder.put(Attributes.ARMOR.value(), new AttributeModifier(attributeId, armor, AttributeModifier.Operation.ADD_VALUE));
      }
      float toughness = statsNBT.get(ToolStats.ARMOR_TOUGHNESS);
      if (toughness > 0) {
        builder.put(Attributes.ARMOR_TOUGHNESS.value(), new AttributeModifier(attributeId, toughness, AttributeModifier.Operation.ADD_VALUE));
      }
      double knockbackResistance = statsNBT.get(ToolStats.KNOCKBACK_RESISTANCE);
      if (knockbackResistance > 0) {
        builder.put(Attributes.KNOCKBACK_RESISTANCE.value(), new AttributeModifier(attributeId, knockbackResistance, AttributeModifier.Operation.ADD_VALUE));
      }
      // grab attributes from modifiers
      BiConsumer<Attribute,AttributeModifier> attributeConsumer = builder::put;
      for (ModifierEntry entry : tool.getModifierList()) {
        entry.getHook(ModifierHooks.ATTRIBUTES).addAttributes(tool, entry, slot, attributeConsumer);
      }
    }

    return builder.build();
  }

  @Override
  public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
    CompoundTag nbt = slimeknights.tconstruct.library.utils.ItemStackDataUtil.getTag(stack);
    if (nbt == null) {
      return ItemAttributeModifiers.EMPTY;
    }
    EquipmentSlot slot = getEquipmentSlot();
    return AttributesModifierHook.toComponent(getAttributeModifiers(ToolStack.from(stack), slot), slot);
  }


  /* Elytra */

  /** Mirrors Tinkers' dynamic wings flag to the component used by vanilla 26.1 gliding. */
  @Override
  public void updateDynamicComponents(ItemStack stack) {
    boolean glider = armorSlotType == ArmorSlotType.CHESTPLATE
      && !ToolDamageUtil.isBroken(stack)
      && ModifierUtil.checkVolatileFlag(stack, ELYTRA);
    if (glider) {
      if (!stack.has(DataComponents.GLIDER)) {
        stack.set(DataComponents.GLIDER, Unit.INSTANCE);
      }
    } else if (stack.has(DataComponents.GLIDER)) {
      stack.remove(DataComponents.GLIDER);
    }

    // A missing modifier must also clear the vanilla mirror. Resolving a new
    // trim requires registry access and is handled by inventoryTick below.
    if (ModifierUtil.getModifierLevel(stack, TinkerModifiers.trim.getId()) <= 0
        && stack.has(DataComponents.TRIM)) {
      stack.remove(DataComponents.TRIM);
    }
  }

  /** Mirrors Tinkers' persistent trim data to the component used by every native armor renderer. */
  private static void updateArmorTrimComponent(ItemStack stack, ToolStack tool, RegistryAccess access) {
    ArmorTrim trim = tool.getModifierLevel(TinkerModifiers.trim.getId()) > 0
      ? TrimModule.getArmorTrim(tool, TinkerModifiers.trim.getId(), access)
      : null;
    if (trim != null) {
      if (!trim.equals(stack.get(DataComponents.TRIM))) {
        stack.set(DataComponents.TRIM, trim);
      }
    } else if (stack.has(DataComponents.TRIM)) {
      stack.remove(DataComponents.TRIM);
    }
  }

  public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
    return armorSlotType == ArmorSlotType.CHESTPLATE && !ToolDamageUtil.isBroken(stack) && ModifierUtil.checkVolatileFlag(stack, ELYTRA);
  }

  public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
    if (getEquipmentSlot() == EquipmentSlot.CHEST) {
      ToolStack tool = ToolStack.from(stack);
      if (!tool.isBroken()) {
        // if any modifier says stop flying, stop flying
        for (ModifierEntry entry : tool.getModifierList()) {
          if (entry.getHook(ModifierHooks.ELYTRA_FLIGHT).elytraFlightTick(tool, entry, entity, flightTicks)) {
            return false;
          }
        }
        // damage the tool and keep flying
        if (!entity.level().isClientSide() && (flightTicks + 1) % 20 == 0) {
          ToolDamageUtil.damageAnimated(tool, 1, entity, EquipmentSlot.CHEST);
        }
        return true;
      }
    }
    return false;
  }


  /* Ticking */

  @Override
  public void inventoryTick(ItemStack stack, ServerLevel levelIn, Entity entityIn, EquipmentSlot slot) {
    // don't care about non-living, they skip most tool context
    if (entityIn instanceof LivingEntity living) {
      ToolStack tool = ToolStack.from(stack);
      if (!levelIn.isClientSide()) {
        tool.ensureHasData();
        // Migrates initialized stacks created before native dynamic component support.
        updateDynamicComponents(stack);
        updateArmorTrimComponent(stack, tool, levelIn.registryAccess());
      }
      List<ModifierEntry> modifiers = tool.getModifierList();
      if (!modifiers.isEmpty()) {
        boolean isCorrectSlot = living.getItemBySlot(getEquipmentSlot()) == stack;
        // we pass in the stack for most custom context, but for the sake of armor its easier to tell them that this is the correct slot for effects
        for (ModifierEntry entry : modifiers) {
          entry.getHook(ModifierHooks.INVENTORY_TICK).onInventoryTick(tool, entry, levelIn, living,
            InventoryTickModifierHook.getItemSlot(slot), InventoryTickModifierHook.isSelected(slot), isCorrectSlot, stack);
        }
      }
    }
  }

  @Override
  public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
    return SlotStackModifierHook.overrideStackedOnOther(held, slot, action, player) || super.overrideStackedOnOther(held, slot, action, player);
  }

  @Override
  public boolean overrideOtherStackedOnMe(ItemStack slotStack, ItemStack held, Slot slot, ClickAction action, Player player, SlotAccess access) {
    return SlotStackModifierHook.overrideOtherStackedOnMe(slotStack, held, slot, action, player, access) || super.overrideOtherStackedOnMe(slotStack, held, slot, action, player, access);
  }


  /* Tooltips */

  @Override
  public Component getName(ItemStack stack) {
    return ToolNameHook.getName(getToolDefinition(), stack);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new ArrayList<>();
    TooltipUtil.addInformation(this, stack, context, lines, SafeClientAccess.getTooltipKey(), flag);
    lines.forEach(tooltip);
  }

  @Override
  public List<Component> getStatInformation(IToolStackView tool, @Nullable Player player, List<Component> tooltips, TooltipKey key, TooltipFlag tooltipFlag) {
    tooltips = TooltipUtil.getArmorStats(tool, player, tooltips, key, tooltipFlag);
    TooltipUtil.addAttributes(this, tool, player, tooltips, TooltipUtil.SHOW_ARMOR_ATTRIBUTES, getEquipmentSlot());
    return tooltips;
  }

  /* Display items */

  @Override
  public ItemStack getRenderTool() {
    if (toolForRendering == null) {
      toolForRendering = ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition());
    }
    return toolForRendering;
  }
}

package slimeknights.tconstruct.fluids.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.tconstruct.fluids.util.ConstantFluidContainerWrapper;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.library.utils.Util;

import java.util.function.Supplier;

/** Implements filling a bucket with an NBT fluid */
public class PotionBucketItem extends PotionItem {
  static final int DRINK_DURATION = 96;
  static final float POTION_DURATION_SCALE = 2.5F;
  private final Supplier<? extends Fluid> supplier;
  public PotionBucketItem(Supplier<? extends Fluid> supplier, Properties builder) {
    super(builder
      .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
      .component(DataComponents.POTION_DURATION_SCALE, POTION_DURATION_SCALE)
      .component(DataComponents.CONSUMABLE, Consumables.defaultDrink().consumeSeconds(DRINK_DURATION / 20.0F).build()));
    this.supplier = supplier;
  }

  public Fluid getFluid() {
    return supplier.get();
  }

  @Override
  public Component getName(ItemStack stack) {
    PotionContents contents = PotionFluidType.getPotionContents(stack);
    var potionHolder = contents.potion().orElse(Potions.WATER);
    String bucketKey = getDescriptionId() + ".effect." + potionHolder.value().name();
    if (Util.canTranslate(bucketKey)) {
      return Component.translatable(bucketKey);
    }
    // default to filling with the contents
    return Component.translatable(getDescriptionId() + ".contents", contents.getName("item.minecraft.potion.effect."));
  }

  @Override
  public ItemStack getDefaultInstance() {
    ItemStack stack = new ItemStack(this);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
    return stack;
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    Player player = living instanceof Player p ? p : null;
    if (player instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
    }

    // bucket potions apply effects at 2.5x duration or instant strength
    if (!level.isClientSide()) {
      for (MobEffectInstance effect : PotionFluidType.getPotionContents(stack).getAllEffects()) {
        if (effect.getEffect().value().isInstantenous()) {
          effect.getEffect().value().applyInstantenousEffect((ServerLevel) level, player, player, living, effect.getAmplifier(), 2.5D);
        } else {
          MobEffectInstance newEffect = new MobEffectInstance(effect.getEffect(), effect.getDuration() * 5 / 2, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
          living.addEffect(newEffect);
        }
      }
    }

    if (player != null) {
      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
        stack.shrink(1);
      }
    }

    if (player == null || !player.getAbilities().instabuild) {
      if (stack.isEmpty()) {
        return new ItemStack(Items.BUCKET);
      }
      if (player != null) {
        player.getInventory().add(new ItemStack(Items.BUCKET));
      }
    }
    living.gameEvent(GameEvent.DRINK);
    return stack;
  }

  @Override
  public int getUseDuration(ItemStack pStack, LivingEntity entity) {
    return DRINK_DURATION; // 3x duration of potion bottles
  }

  public ResourceHandler<FluidResource> getFluidHandler(ItemAccess access) {
    ItemResource filled = access.getResource();
    FluidStack fluid = new FluidStack(getFluid(), FluidType.BUCKET_VOLUME);
    if (!filled.isComponentsPatchEmpty()) {
      fluid.applyComponents(filled.getComponentsPatch());
    }
    return new ConstantFluidContainerWrapper(fluid, access, ItemResource.of(Items.BUCKET));
  }
}

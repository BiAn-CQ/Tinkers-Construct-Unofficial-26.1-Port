package slimeknights.tconstruct.tools.modules.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForgeMod;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.AreaOfEffectHighlightModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator.AOEMatchType;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;

/** Modifier module to implement behavior of {@link BrushItem} */
public enum BrushModule implements ModifierModule, GeneralInteractionModifierHook, BlockInteractionModifierHook, AreaOfEffectHighlightModifierHook {
  INSTANCE;

  public static final RecordLoadable<BrushModule> LOADER = new SingletonLoader<>(INSTANCE);
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<BrushModule>defaultHooks(ModifierHooks.GENERAL_INTERACT, ModifierHooks.BLOCK_INTERACT, ModifierHooks.AOE_HIGHLIGHT);

  @Override
  public RecordLoadable<BrushModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
    // runs on block click
    return InteractionResult.PASS;
  }

  /** Runs an entity raytrace for brushing. See same method on {@link BrushItem} */
  private static HitResult calculateHitResult(LivingEntity living) {
    return ProjectileUtil.getHitResultOnViewVector(living, entity -> !entity.isSpectator() && entity.isPickable(), living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE));
  }

  @Override
  public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
    Player player = context.getPlayer();
    // must have a player, and no entity in the way
    if (source == InteractionSource.RIGHT_CLICK && player != null && calculateHitResult(player).getType() == HitResult.Type.BLOCK) {
      GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, context.getHand());
    }
    return InteractionResult.CONSUME;
  }

  @Override
  public ItemUseAnimation getUseAction(IToolStackView tool, ModifierEntry modifier) {
    return ModifierUtil.blockWhileCharging(tool, ItemUseAnimation.BRUSH);
  }

  @Override
  public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
    return 200;
  }

  /** Plays sound and shows particles */
  private static void brushEffects(Player player, BlockHitResult blockHit, BlockState state, HumanoidArm arm, SoundEvent sound) {
    Level level = player.level();

    // spawn particles
    spawnDustParticles(level, blockHit, state, player.getViewVector(0.0F), arm);

    // play sound
    level.playSound(player, blockHit.getBlockPos(), sound, SoundSource.BLOCKS);
  }

  /**
   * 26.1 made the vanilla brush particle helper private. Keep the same small
   * effect here so modifier-driven brushing retains vanilla feedback without
   * depending on a private implementation detail.
   */
  private static void spawnDustParticles(Level level, BlockHitResult blockHit, BlockState state, net.minecraft.world.phys.Vec3 viewVector, HumanoidArm arm) {
    int count = level.getRandom().nextInt(7, 12);
    BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
    Direction direction = blockHit.getDirection();
    double xd;
    double zd;
    switch (direction) {
      case DOWN, UP -> {
        xd = viewVector.z();
        zd = -viewVector.x();
      }
      case NORTH -> {
        xd = 1.0;
        zd = -0.1;
      }
      case SOUTH -> {
        xd = -1.0;
        zd = 0.1;
      }
      case WEST -> {
        xd = -0.1;
        zd = -1.0;
      }
      case EAST -> {
        xd = 0.1;
        zd = 1.0;
      }
      default -> throw new IllegalStateException("Unexpected brush direction: " + direction);
    }
    int armSign = arm == HumanoidArm.RIGHT ? 1 : -1;
    net.minecraft.world.phys.Vec3 location = blockHit.getLocation();
    for (int i = 0; i < count; i++) {
      double x = location.x() - (direction == Direction.WEST ? 1.0E-6 : 0.0) + xd * armSign * 3.0 * level.getRandom().nextDouble();
      double y = location.y();
      double z = location.z() - (direction == Direction.NORTH ? 1.0E-6 : 0.0) + zd * armSign * 3.0 * level.getRandom().nextDouble();
      level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
    }
  }

  /** Brushes a single block */
  private static boolean brushBlock(Player player, BlockHitResult blockHit, BlockState state, HumanoidArm arm) {
    // only play sound and particle if its a brushable block, reduces noice on AOE
    Level level = player.level();
    if (state.getBlock() instanceof BrushableBlock brushable) {
      brushEffects(player, blockHit, state, arm, brushable.getBrushSound());
      if (level.isClientSide()) {
        return true;
      }
    }

    // brush the block
    if (level instanceof ServerLevel serverLevel) {
      return level.getBlockEntity(blockHit.getBlockPos()) instanceof BrushableBlockEntity brushable
        && brushable.brush(level.getGameTime(), serverLevel, player, blockHit.getDirection(), player.getItemInHand(player.getUsedItemHand()));
    }
    return false;
  }

  @Override
  public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
    // must not be out of time
    if (timeLeft >= 0 && entity instanceof Player player) {
      // find the block we hit
      HitResult hit = calculateHitResult(entity);
      if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
        // from this point on we have succeeded at interaction so will not stop using, but we still don't act every tick
        if ((getUseDuration(tool, modifier) - timeLeft + 1) % 10 == 5) {
          Level level = entity.level();
          BlockPos pos = blockHit.getBlockPos();
          BlockState state = level.getBlockState(pos);
          InteractionHand hand = entity.getUsedItemHand();
          HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();

          // brush main block
          int damage = 0;
          if (brushBlock(player, blockHit, state, arm)) {
            damage += 1;
          }

          // brush AOE blocks
          UseOnContext context = new UseOnContext(level, player, hand, player.getItemInHand(hand), blockHit);
          for (BlockPos extraPos : tool.getDefinition().getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, context, state, AOEMatchType.TRANSFORM)) {
            if (brushBlock(player, Util.offset(blockHit, extraPos), level.getBlockState(extraPos), arm)) {
              damage += 1;
            }
          }

          // if nothing was brushed clientside, play the effect for the center block
          if (damage == 0 && level.isClientSide()) {
            brushEffects(player, blockHit, state, arm, SoundEvents.BRUSH_GENERIC);
          }

          // apply all tool damage, and stop using if needed
          if (damage > 0 && !level.isClientSide() && ToolDamageUtil.damageAnimated(tool, damage, entity, hand, modifier.getId())) {
            entity.stopUsingItem();
          }
        }
        return;
      }
    }
    entity.releaseUsingItem();
  }

  @Override
  public boolean shouldHighlight(IToolStackView tool, ModifierEntry modifier, UseOnContext context, BlockPos offset, BlockState state) {
    return context.getLevel().getBlockEntity(offset) instanceof BrushableBlockEntity;
  }
}

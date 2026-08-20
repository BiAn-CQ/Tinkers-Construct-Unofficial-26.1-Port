package slimeknights.tconstruct.library.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Explosion implementation used by Tinkers for configurable damage and
 * knockback.  Minecraft 26.1 changed {@link Explosion} into an interface and
 * moved the vanilla implementation to {@link ServerExplosion}; keeping this
 * implementation independent avoids relying on vanilla's private state.
 */
public class CustomExplosion extends ServerExplosion {
  private static final int RAY_COUNT = 16;
  private static final int MAX_RAY = RAY_COUNT - 1;
  private static final WeightedList<ExplosionParticleInfo> BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
    .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
    .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
    .build();

  public static final Predicate<Entity> DEFAULT_ENTITY_PREDICATE = entity -> entity != null && entity.isAlive() && !entity.isSpectator();

  protected final ServerLevel level;
  protected final Vec3 center;
  protected final float radius;
  protected final boolean fire;
  @Nullable
  protected final Entity source;
  protected final DamageSource damageSource;
  protected final ExplosionDamageCalculator damageCalculator;
  protected final Map<Player,Vec3> hitPlayers = new HashMap<>();
  protected final List<BlockPos> toBlow = new java.util.ArrayList<>();
  protected final float damage;
  protected final float knockback;
  protected final Predicate<Entity> entityPredicate;
  protected final boolean bypassInvulnerableTime;
  protected final Explosion.BlockInteraction blockInteraction;

  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity,
                         @Nullable Predicate<Entity> entityPredicate, float damage,
                         @Nullable DamageSource damageSource, float knockback,
                         @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire,
                         Explosion.BlockInteraction blockInteraction, boolean bypassInvulnerableTime) {
    super(requireServerLevel(level), sourceEntity,
      damageSource != null ? damageSource : Explosion.getDefaultDamageSource(requireServerLevel(level), sourceEntity),
      damageCalculator != null ? damageCalculator : new ExplosionDamageCalculator(),
      location, radius, placeFire, blockInteraction);
    ServerLevel serverLevel = requireServerLevel(level);
    this.level = serverLevel;
    this.center = location;
    this.radius = radius;
    this.fire = placeFire;
    this.source = sourceEntity;
    this.damageSource = damageSource != null ? damageSource : Explosion.getDefaultDamageSource(serverLevel, sourceEntity);
    this.damageCalculator = damageCalculator != null ? damageCalculator : new ExplosionDamageCalculator();
    this.entityPredicate = Objects.requireNonNullElse(entityPredicate, DEFAULT_ENTITY_PREDICATE);
    this.damage = damage;
    this.knockback = knockback;
    this.bypassInvulnerableTime = bypassInvulnerableTime;
    this.blockInteraction = blockInteraction;
  }

  private static ServerLevel requireServerLevel(Level level) {
    if (level instanceof ServerLevel serverLevel) {
      return serverLevel;
    }
    throw new IllegalArgumentException("Custom explosions require a server level");
  }

  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity,
                         @Nullable Predicate<Entity> entityPredicate, float damage,
                         @Nullable DamageSource damageSource, float knockback,
                         @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire,
                         Explosion.BlockInteraction blockInteraction) {
    this(level, location, radius, sourceEntity, entityPredicate, damage, damageSource, knockback,
      damageCalculator, placeFire, blockInteraction, false);
  }

  @Override
  public ServerLevel level() {
    return level;
  }

  @Override
  public Explosion.BlockInteraction getBlockInteraction() {
    return blockInteraction;
  }

  @Override
  public DamageSource getDamageSource() {
    return damageSource;
  }

  @Override
  public @Nullable LivingEntity getIndirectSourceEntity() {
    return Explosion.getIndirectSourceEntity(source);
  }

  @Override
  public @Nullable Entity getDirectSourceEntity() {
    return source;
  }

  @Override
  public float radius() {
    return radius;
  }

  @Override
  public Vec3 center() {
    return center;
  }

  @Override
  public boolean canTriggerBlocks() {
    return blockInteraction == Explosion.BlockInteraction.TRIGGER_BLOCK;
  }

  @Override
  public boolean shouldAffectBlocklikeEntities() {
    return blockInteraction.shouldAffectBlocklikeEntities();
  }

  /** Calculates the list of blocks to hit. */
  protected void calculateHitBlocks() {
    if (!interactsWithBlocks() && !fire) {
      return;
    }

    Set<BlockPos> set = new HashSet<>();
    for (int rayX = 0; rayX < RAY_COUNT; rayX++) {
      for (int rayY = 0; rayY < RAY_COUNT; rayY++) {
        for (int rayZ = 0; rayZ < RAY_COUNT; rayZ++) {
          if (rayX == 0 || rayX == MAX_RAY || rayY == 0 || rayY == MAX_RAY || rayZ == 0 || rayZ == MAX_RAY) {
            double stepX = rayX * 2.0 / MAX_RAY - 1;
            double stepY = rayY * 2.0 / MAX_RAY - 1;
            double stepZ = rayZ * 2.0 / MAX_RAY - 1;
            double stepScale = 0.3f / Math.sqrt(stepX * stepX + stepY * stepY + stepZ * stepZ);
            stepX *= stepScale;
            stepY *= stepScale;
            stepZ *= stepScale;

            double targetX = center.x;
            double targetY = center.y;
            double targetZ = center.z;
            for (float power = radius * (0.7f + level.getRandom().nextFloat() * 0.6f); power > 0; power -= 0.225f) {
              BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
              if (!level.isInWorldBounds(target)) {
                break;
              }
              BlockState block = level.getBlockState(target);
              FluidState fluid = level.getFluidState(target);
              Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(this, level, target, block, fluid);
              if (resistance.isPresent()) {
                power -= (resistance.get() + 0.3f) * 0.3f;
              }
              if ((fire || !block.isAir()) && power > 0 && damageCalculator.shouldBlockExplode(this, level, target, block, power)) {
                set.add(target);
              }
              targetX += stepX;
              targetY += stepY;
              targetZ += stepZ;
            }
          }
        }
      }
    }
    toBlow.addAll(set);
  }

  /** Damages and knocks back entities using the 26.1 server-side APIs. */
  protected void damageAndPushEntities() {
    if (damage <= 0 && knockback == 0) {
      return;
    }
    float diameter = radius * 2;
    List<Entity> entities = level.getEntities(source, new AABB(
      Mth.floor(center.x - diameter - 1), Mth.floor(center.y - diameter - 1), Mth.floor(center.z - diameter - 1),
      Mth.floor(center.x + diameter + 1), Mth.floor(center.y + diameter + 1), Mth.floor(center.z + diameter + 1)),
      entityPredicate);
    EventHooks.onExplosionDetonate(level, this, entities, toBlow);
    for (Entity entity : entities) {
      if (entity.ignoreExplosion(this)) {
        continue;
      }
      Vec3 direction = (entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition()).subtract(center);
      double distance = direction.length() / diameter;
      if (distance > 1) {
        continue;
      }
      double length = direction.length();
      if (length <= 1.0E-4D) {
        continue;
      }
      float strength = (float)((1 - distance) * ServerExplosion.getSeenPercent(center, entity));
      if (damage > 0) {
        float amount = (float)((strength * strength + strength) / 2 * damage + 1);
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
          if (bypassInvulnerableTime) {
            ToolAttackUtil.hurtNoInvulnerableTime(player, damageSource, amount);
          } else {
            player.hurtServer(level, damageSource, amount);
          }
        } else if (entity instanceof LivingEntity living) {
          living.hurtServer(level, damageSource, amount);
        }
      }
      if (knockback != 0) {
        double adjustedStrength = strength * knockback;
        if (entity instanceof LivingEntity living && living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) >= 1) {
          adjustedStrength = 0;
        }
        Vec3 velocity = direction.scale(adjustedStrength / length);
        velocity = EventHooks.getExplosionKnockback(level, this, entity, velocity, toBlow);
        entity.push(velocity);
        if (entity instanceof Player player && !player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
          hitPlayers.put(player, velocity);
        }
      }
      if (entity.typeHolder().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
        projectile.setOwner(damageSource.getEntity());
      }
      entity.onExplosionHit(source);
    }
  }

  /** Executes the explosion and sends the 26.1 packet to nearby players. */
  public void handleServer() {
    if (EventHooks.onExplosionStart(level, this)) {
      return;
    }
    level.gameEvent(source, GameEvent.EXPLODE, center);
    calculateHitBlocks();
    damageAndPushEntities();
    if (interactsWithBlocks()) {
      for (BlockPos pos : toBlow) {
        level.getBlockState(pos).onExplosionHit(level, pos, this, (stack, position) -> net.minecraft.world.level.block.Block.popResource(level, position, stack));
      }
    }
    if (fire) {
      for (BlockPos pos : toBlow) {
        if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender()) {
          level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
      }
    }
    syncToClient();
  }

  /** Executes the same logic on either logical side for legacy callers. */
  public void doDualSide(Level level, boolean spawnParticles) {
    handleServer();
  }

  protected boolean interactsWithBlocks() {
    return blockInteraction != Explosion.BlockInteraction.KEEP;
  }

  private void syncToClient() {
    ParticleOptions particle = radius < 2 || !interactsWithBlocks() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER;
    Holder<SoundEvent> sound = SoundEvents.GENERIC_EXPLODE;
    for (ServerPlayer player : level.players()) {
      if (player.distanceToSqr(center) < 4096.0D) {
        player.connection.send(new ClientboundExplodePacket(center, radius, toBlow.size(), Optional.ofNullable(hitPlayers.get(player)), particle, sound, BLOCK_PARTICLES));
      }
    }
  }
}

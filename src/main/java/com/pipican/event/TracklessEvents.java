package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Silent steps, mobs losing track of you, wind charges that pull, and explosions that break more. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class TracklessEvents {

    private static final List<Holder<GameEvent>> STEPS = List.of(GameEvent.STEP, GameEvent.HIT_GROUND, GameEvent.SWIM, GameEvent.SPLASH);
    /** Mob -> (game time, velocity) of the push-out that follows a wind charge's pull. */
    private record Pending(long at, Vec3 velocity) {}
    private static final Map<UUID, Pending> PUSH_OUT = new ConcurrentHashMap<>();

    private TracklessEvents() {}

    /** {@code no_sculk_steps}: your footsteps (and landing, swimming, splashing) make no vibration for sculk to hear. */
    @SubscribeEvent
    static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player && STEPS.stream().anyMatch(event.getVanillaEvent()::is)
                && ClassEffectApplier.state(player).chance(Kind.NO_SCULK_STEPS) > 0) {
            event.setCanceled(true);
        }
    }

    /** {@code lose_track}: every few seconds, mobs after you forget you. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double every = ClassEffectApplier.state(player).chance(Kind.LOSE_TRACK);
        if (every <= 0 || player.tickCount % Math.max(20, Math.round(every * 20)) != 0) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    /** {@code wind_pull} (Cyclone): light mobs are drawn in first; the push out comes half a second later. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOW)
    static void onWindKnockback(ExplosionKnockbackEvent event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge charge)
                || !(charge.getOwner() instanceof ServerPlayer player) || !(event.getAffectedEntity() instanceof LivingEntity mob)
                || mob instanceof Player || mob.getMaxHealth() > 20 || ClassEffectApplier.state(player).chance(Kind.WIND_PULL) <= 0) {
            return;
        }
        Vec3 out = event.getKnockbackVelocity();
        Vec3 in = event.getExplosion().center().subtract(mob.position()).multiply(1, 0, 1);
        event.setKnockbackVelocity(in.lengthSqr() < 1.0e-4 ? Vec3.ZERO : in.normalize().scale(Math.min(0.8, in.length() * 0.4)));
        PUSH_OUT.put(mob.getUUID(), new Pending(mob.level().getGameTime() + 10, out));
    }

    @SubscribeEvent
    static void onMobTick(EntityTickEvent.Post event) {
        if (PUSH_OUT.isEmpty() || !(event.getEntity() instanceof LivingEntity mob) || mob.level().isClientSide()) {
            return;
        }
        Pending p = PUSH_OUT.get(mob.getUUID());
        if (p != null && mob.level().getGameTime() >= p.at()) {
            PUSH_OUT.remove(mob.getUUID());
            mob.setDeltaMovement(mob.getDeltaMovement().add(p.velocity()));
            mob.hurtMarked = true;
        }
    }

    /** {@code blast_grief}: blocks-breaking explosions you set off reach this much farther into blocks. */
    @SubscribeEvent
    static void onDetonate(ExplosionEvent.Detonate event) {
        var explosion = event.getExplosion();
        if (!(explosion.getIndirectSourceEntity() instanceof ServerPlayer player) || explosion.getBlockInteraction() == Explosion.BlockInteraction.KEEP
                || explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double more = ClassEffectApplier.state(player).chance(Kind.BLAST_GRIEF);
        if (more <= 0) {
            return;
        }
        double r = explosion.radius() * (1 + more);
        Vec3 c = explosion.center();
        var already = new java.util.HashSet<>(event.getAffectedBlocks());
        int reach = (int) Math.ceil(r);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(c).offset(-reach, -reach, -reach), BlockPos.containing(c).offset(reach, reach, reach))) {
            if (pos.getCenter().distanceToSqr(c) > r * r || already.contains(pos)) {
                continue;
            }
            var state = level.getBlockState(pos);
            // Only what an explosion could break anyway: not air, fluids, or blast-proof blocks (obsidian and up).
            if (!state.isAir() && state.getFluidState().isEmpty() && state.getBlock().getExplosionResistance() < 1200) {
                event.getAffectedBlocks().add(pos.immutable());
            }
        }
    }
}

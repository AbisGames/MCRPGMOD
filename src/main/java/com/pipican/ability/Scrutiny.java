package com.pipican.ability;

import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** {@code pipican:scrutiny}: keep looking at a mob long enough and it is marked (and glows). */
public final class Scrutiny {

    private record Look(UUID target, long since) {}

    private static final Map<UUID, Look> LOOKING = new ConcurrentHashMap<>();
    /** Player -> the mobs they have scrutinised, oldest first. */
    private static final Map<UUID, java.util.ArrayDeque<LivingEntity>> SCRUTINISED = new ConcurrentHashMap<>();

    private Scrutiny() {}

    /** Every 5 ticks. */
    public static void tick(ServerPlayer player) {
        ClassState.Scrutiny s = ClassEffectApplier.state(player).scrutiny();
        // Dreadful Threat: scrutinised mobs near you are weakened.
        double weaken = ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.SCRUTINY_WEAKNESS);
        if (weaken > 0 && s.seconds() > 0) {
            for (LivingEntity near : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(weaken),
                    e -> e != player && !(e instanceof Player) && e.distanceToSqr(player) <= weaken * weaken && Marks.isMarkedBy(e, player))) {
                near.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true), player);
            }
        }
        if (s.seconds() <= 0) {
            LOOKING.remove(player.getUUID());
            return;
        }
        LivingEntity target = Targets.lookTarget(player, 24);
        if (target == null || target instanceof Player) {
            LOOKING.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Look look = LOOKING.get(player.getUUID());
        if (look == null || !look.target.equals(target.getUUID())) {
            LOOKING.put(player.getUUID(), new Look(target.getUUID(), now));
            return;
        }
        if (now - look.since >= Math.max(0, s.lookSeconds()) * 20 && !Marks.isMarkedBy(target, player)) {
            long ticks = Math.round(s.seconds() * 20);
            // One scrutinised mob at a time (more with scrutiny_targets): a new one replaces the oldest.
            var mine = SCRUTINISED.computeIfAbsent(player.getUUID(), k -> new java.util.ArrayDeque<>());
            mine.removeIf(e -> !e.isAlive() || !Marks.isMarkedBy(e, player));
            int cap = 1 + (int) Math.floor(ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.SCRUTINY_TARGETS) + 1e-9);
            while (mine.size() >= cap) {
                LivingEntity old = mine.pollFirst();
                Marks.remove(old, player);
                old.removeEffect(MobEffects.GLOWING);
            }
            mine.addLast(target);
            Marks.add(target, player, ticks, 1.0, false, false, false);
            double expose = ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.SCRUTINY_EXPOSE);
            if (expose > 0) {
                // Expose: everyone's hits on it are stronger (a shared mark).
                Marks.add(target, player, ticks, 1 + expose, true, false, false);
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) ticks, 0, false, false), player);
        }
    }

    public static void forget(ServerPlayer player) {
        LOOKING.remove(player.getUUID());
        SCRUTINISED.remove(player.getUUID());
    }
}

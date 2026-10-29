package com.pipican.ability;

import com.pipican.effect.type.QuarryEffect.Pick;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The mobs players have chosen with {@code pipican:quarry} (vows, prey, challenged foes). A quarry also counts as
 * marked by its player, so "marked target" bonuses apply to it. Not saved: quarries end on restart.
 */
public final class Quarries {

    private static final class Active {
        final LivingEntity target;
        final long endsAt;
        boolean hitYou;

        Active(LivingEntity target, long endsAt) {
            this.target = target;
            this.endsAt = endsAt;
        }
    }

    private static final Map<UUID, Map<String, Active>> ACTIVE = new ConcurrentHashMap<>();

    private Quarries() {}

    private static boolean live(Active a, long now) {
        return a != null && a.target.isAlive() && !a.target.isRemoved() && a.endsAt > now;
    }

    /** True if {@code player} has a live quarry called {@code name}. */
    public static boolean has(ServerPlayer player, String name) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        return mine != null && live(mine.get(name), player.level().getGameTime());
    }

    /** {@code player} hit {@code target}: ends quarries that end on another hit, then picks by hit. */
    public static void onHit(ServerPlayer player, LivingEntity target, boolean projectile, boolean melee) {
        if (target instanceof Player) {
            return;
        }
        Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(player).quarries();
        if (quarries.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        quarries.forEach((name, q) -> {
            Active a = mine == null ? null : mine.get(name);
            if (q.endOnOtherHit() && live(a, now) && a.target != target) {
                end(player, name);
            }
            if (projectile && q.pick() == Pick.ARROW_HIT || melee && q.melee() || q.pick() == Pick.FIRST_HIT) {
                set(player, name, q, target);
            }
        });
    }

    /** A mob hurt {@code victim}: players within 16 blocks with a {@code hurt_by} quarry may choose it. */
    public static void onPlayerHurt(ServerPlayer victim, LivingEntity attacker) {
        if (attacker instanceof Player) {
            return;
        }
        for (ServerPlayer p : victim.level().getEntitiesOfClass(ServerPlayer.class, victim.getBoundingBox().inflate(16), p -> p.distanceToSqr(victim) <= 256)) {
            ClassEffectApplier.state(p).quarries().forEach((name, q) -> {
                if (q.pick() == Pick.HURT_BY) {
                    set(p, name, q, attacker);
                }
            });
        }
        Map<String, Active> mine = ACTIVE.get(victim.getUUID());
        if (mine != null) {
            mine.values().forEach(a -> {
                if (a.target == attacker) {
                    a.hitYou = true;
                }
            });
        }
    }

    private static void set(ServerPlayer player, String name, ClassState.Quarry q, LivingEntity target) {
        long now = player.level().getGameTime();
        Map<String, Active> mine = ACTIVE.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        Active old = mine.get(name);
        if (live(old, now) && (!q.replace() || old.target == target)) {
            return;
        }
        if (old != null) {
            Marks.remove(old.target, player);
        }
        long ticks = q.forever() ? Long.MAX_VALUE / 4 : Math.round(q.seconds() * 20);
        mine.put(name, new Active(target, now + ticks));
        Marks.add(target, player, ticks, 1.0, false, false, false);
        int duration = q.forever() || ticks > Integer.MAX_VALUE ? MobEffectInstance.INFINITE_DURATION : (int) ticks;
        if (q.glow()) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false), player);
        }
        q.effects().forEach((effect, level) -> target.addEffect(new MobEffectInstance(effect, duration, level), player));
    }

    private static void end(ServerPlayer player, String name) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        Active a = mine == null ? null : mine.remove(name);
        if (a != null) {
            Marks.remove(a.target, player);
            a.target.removeEffect(MobEffects.GLOWING);
        }
    }

    /** True if {@code entity} is one of {@code player}'s live quarries. */
    public static boolean isQuarryOf(LivingEntity entity, ServerPlayer player) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        long now = player.level().getGameTime();
        return mine != null && mine.values().stream().anyMatch(a -> a.target == entity && live(a, now));
    }

    /** Damage multiplier for {@code attacker} hitting {@code victim}. */
    public static double multiplier(ServerPlayer attacker, LivingEntity victim) {
        Map<String, Active> mine = ACTIVE.get(attacker.getUUID());
        if (mine == null) {
            return 1;
        }
        long now = attacker.level().getGameTime();
        Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(attacker).quarries();
        double mult = 1;
        for (var entry : mine.entrySet()) {
            Active a = entry.getValue();
            ClassState.Quarry q = quarries.get(entry.getKey());
            if (q != null && a.target == victim && live(a, now)) {
                mult *= q.multiplier() * (a.hitYou ? 1 : 1 + q.untouched());
            }
        }
        return mult;
    }

    /** Multiplier for damage {@code attacker} deals to {@code victim}: challenged foes hit others for less. */
    public static double othersMultiplier(LivingEntity attacker, LivingEntity victim, MinecraftServer server) {
        double mult = 1;
        long now = attacker.level().getGameTime();
        for (var entry : ACTIVE.entrySet()) {
            if (entry.getKey().equals(victim.getUUID())) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
                continue;
            }
            Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(owner).quarries();
            for (var a : entry.getValue().entrySet()) {
                ClassState.Quarry q = quarries.get(a.getKey());
                if (q != null && q.othersDamage() > 0 && a.getValue().target == attacker && live(a.getValue(), now)) {
                    mult *= 1 - q.othersDamage();
                }
            }
        }
        return mult;
    }

    /** Every second: forgets ended quarries. */
    public static void tick(MinecraftServer server) {
        ACTIVE.forEach((uuid, mine) -> {
            mine.values().removeIf(a -> !live(a, a.target.level().getGameTime()));
            if (mine.isEmpty()) {
                ACTIVE.remove(uuid);
            }
        });
    }
}

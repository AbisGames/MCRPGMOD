package com.pipican.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Marks placed on mobs by {@code pipican:mark} (Mark Target, Sentence, Called Shot...). A mark makes
 * the marker's damage to that mob bigger; {@code shared} marks help every player, {@code next_hit}
 * marks are used up by the first hit, {@code projectile_only} ones only count for arrows/bolts.
 * Not saved.
 */
public final class Marks {

    record Mark(UUID owner, long endsAt, double multiplier, boolean shared, boolean nextHit, boolean projectileOnly) {}

    private static final Map<UUID, List<Mark>> MARKS = new ConcurrentHashMap<>();

    private Marks() {}

    public static void add(LivingEntity target, Player owner, long ticks, double multiplier, boolean shared, boolean nextHit, boolean projectileOnly) {
        MARKS.computeIfAbsent(target.getUUID(), k -> new ArrayList<>())
                .add(new Mark(owner.getUUID(), target.level().getGameTime() + ticks, multiplier, shared, nextHit, projectileOnly));
    }

    /** True if {@code player} has a mark of their own on {@code target} (for "marked target" bonuses). */
    public static boolean isMarkedBy(LivingEntity target, Player player) {
        List<Mark> list = MARKS.get(target.getUUID());
        long now = target.level().getGameTime();
        return list != null && list.stream().anyMatch(m -> m.owner().equals(player.getUUID()) && m.endsAt() > now);
    }

    /** Damage multiplier for {@code attacker} hitting {@code target}; uses up next-hit marks. */
    public static double multiplier(Player attacker, LivingEntity target, DamageSource source) {
        List<Mark> list = MARKS.get(target.getUUID());
        if (list == null) {
            return 1;
        }
        long now = target.level().getGameTime();
        boolean projectile = source.getDirectEntity() != source.getEntity();
        double mult = 1;
        synchronized (list) {
            for (Iterator<Mark> it = list.iterator(); it.hasNext(); ) {
                Mark m = it.next();
                if (m.endsAt() <= now) {
                    it.remove();
                    continue;
                }
                if ((m.shared() || m.owner().equals(attacker.getUUID())) && (!m.projectileOnly() || projectile)) {
                    mult *= m.multiplier();
                    if (m.nextHit()) {
                        it.remove();
                    }
                }
            }
            if (list.isEmpty()) {
                MARKS.remove(target.getUUID());
            }
        }
        return mult;
    }

    /** True if {@code owner} has a live mark on any mob (a curse you placed is active). */
    public static boolean hasAny(Player owner) {
        long now = owner.level().getGameTime();
        for (List<Mark> list : MARKS.values()) {
            synchronized (list) {
                if (list.stream().anyMatch(m -> m.owner().equals(owner.getUUID()) && m.endsAt() > now)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Removes {@code owner}'s marks from {@code target}. */
    public static void remove(LivingEntity target, Player owner) {
        List<Mark> list = MARKS.get(target.getUUID());
        if (list != null) {
            synchronized (list) {
                list.removeIf(m -> m.owner().equals(owner.getUUID()));
            }
        }
    }

    public static void forget(LivingEntity entity) {
        MARKS.remove(entity.getUUID());
    }
}

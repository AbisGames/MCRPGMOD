package com.pipican.ability;

import com.pipican.effect.ClassEffect;
import com.pipican.player.ClassService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Short-lived class effects from {@code pipican:boost} ("+15% movement speed for 2s after you take damage").
 * Not saved: they last seconds. Using the same boost again restarts its timer instead of stacking.
 */
public final class Boosts {

    private record Boost(Object source, List<ClassEffect> effects, long until, int stacks) {}

    private static final Map<UUID, List<Boost>> ACTIVE = new ConcurrentHashMap<>();

    private Boosts() {}

    public static void add(ServerPlayer player, Object source, List<ClassEffect> effects, int ticks) {
        add(player, source, effects, ticks, 1);
    }

    /** With {@code maxStacks} above 1, using it again while it runs adds a stack (up to the max) and restarts the timer. */
    public static void add(ServerPlayer player, Object source, List<ClassEffect> effects, int ticks, int maxStacks) {
        List<Boost> list = ACTIVE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        int stacks = 1;
        for (Boost b : list) {
            if (b.source == source) {
                stacks = Math.min(Math.max(1, maxStacks), b.stacks + 1);
            }
        }
        list.removeIf(b -> b.source == source);
        list.add(new Boost(source, effects, player.level().getGameTime() + ticks, stacks));
        ClassService.changed(player);
    }

    /** How many stacks of {@code source} are running (0 = none). */
    public static int stacks(ServerPlayer player, Object source) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        return list == null ? 0 : list.stream().filter(b -> b.source == source).mapToInt(Boost::stacks).max().orElse(0);
    }

    /** The effects of every running boost (read while bonuses are rebuilt). */
    public static List<ClassEffect> effects(ServerPlayer player) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<ClassEffect> out = new ArrayList<>();
        for (Boost b : list) {
            b.effects.forEach(e -> out.add(b.stacks > 1 ? new com.pipican.effect.ScaledEffect(e, b.stacks, null) : e));
        }
        return out;
    }

    /** Ends expired boosts (every player tick). */
    public static void tick(ServerPlayer player) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        if (list != null && list.removeIf(b -> player.level().getGameTime() >= b.until)) {
            ClassService.changed(player);
        }
    }

    /** On waking: ends boosts that last "until you sleep". */
    public static void onWake(ServerPlayer player) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        if (list != null && list.removeIf(b -> b.source instanceof com.pipican.ability.action.BoostAction a && a.untilSleep())) {
            ClassService.changed(player);
        }
    }

    public static void forget(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null) {
            ClassService.changed(player);
        }
    }
}

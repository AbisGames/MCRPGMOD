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

    private record Boost(Object source, List<ClassEffect> effects, long until) {}

    private static final Map<UUID, List<Boost>> ACTIVE = new ConcurrentHashMap<>();

    private Boosts() {}

    public static void add(ServerPlayer player, Object source, List<ClassEffect> effects, int ticks) {
        List<Boost> list = ACTIVE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.removeIf(b -> b.source == source);
        list.add(new Boost(source, effects, player.level().getGameTime() + ticks));
        ClassService.changed(player);
    }

    /** The effects of every running boost (read while bonuses are rebuilt). */
    public static List<ClassEffect> effects(ServerPlayer player) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<ClassEffect> out = new ArrayList<>();
        list.forEach(b -> out.addAll(b.effects));
        return out;
    }

    /** Ends expired boosts (every player tick). */
    public static void tick(ServerPlayer player) {
        List<Boost> list = ACTIVE.get(player.getUUID());
        if (list != null && list.removeIf(b -> player.level().getGameTime() >= b.until)) {
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

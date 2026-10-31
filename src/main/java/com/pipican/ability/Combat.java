package com.pipican.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Remembers when each player last hit or was hit by a mob or player ("in combat" for 5 seconds after). */
public final class Combat {

    public static final int TICKS = 100;
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private Combat() {}

    private static final Map<UUID, Long> START = new ConcurrentHashMap<>();

    public static void mark(ServerPlayer player) {
        if (!inCombat(player)) {
            START.put(player.getUUID(), player.level().getGameTime());
        }
        LAST.put(player.getUUID(), player.level().getGameTime());
    }

    /** Ticks since this fight started, or -1 when out of combat. */
    public static long fightTicks(ServerPlayer player) {
        Long start = START.get(player.getUUID());
        return inCombat(player) && start != null ? player.level().getGameTime() - start : -1;
    }

    public static boolean inCombat(ServerPlayer player) {
        Long last = LAST.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < TICKS;
    }

    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}

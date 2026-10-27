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

    public static void mark(ServerPlayer player) {
        LAST.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean inCombat(ServerPlayer player) {
        Long last = LAST.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < TICKS;
    }

    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}

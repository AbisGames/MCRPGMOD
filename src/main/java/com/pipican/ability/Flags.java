package com.pipican.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Named timers set by {@code pipican:flag} and read by the {@code pipican:flag} condition ("can't go Berserk again for 15s"). */
public final class Flags {

    private static final Map<UUID, Map<String, Long>> FLAGS = new ConcurrentHashMap<>();

    private Flags() {}

    public static void set(ServerPlayer player, String name, long ticks) {
        FLAGS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(name, player.level().getGameTime() + ticks);
    }

    public static boolean has(ServerPlayer player, String name) {
        Map<String, Long> mine = FLAGS.get(player.getUUID());
        Long until = mine == null ? null : mine.get(name);
        return until != null && until > player.level().getGameTime();
    }
}

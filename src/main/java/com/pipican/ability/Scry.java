package com.pipican.ability;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Players currently seeing through someone else's eyes ({@code pipican:scry}), and when that ends. */
public final class Scry {

    private static final Map<UUID, Long> WATCHING = new ConcurrentHashMap<>();

    private Scry() {}

    public static void start(ServerPlayer viewer, ServerPlayer target, long ticks) {
        viewer.setCamera(target);
        WATCHING.put(viewer.getUUID(), viewer.level().getGameTime() + ticks);
    }

    public static void tick(MinecraftServer server) {
        for (Iterator<Map.Entry<UUID, Long>> it = WATCHING.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer viewer = server.getPlayerList().getPlayer(e.getKey());
            if (viewer == null) {
                it.remove();
            } else if (viewer.level().getGameTime() >= e.getValue() || viewer.getCamera().isRemoved()) {
                viewer.setCamera(viewer);
                it.remove();
            }
        }
    }
}

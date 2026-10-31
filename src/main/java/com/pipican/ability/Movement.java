package com.pipican.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Remembers when each player last moved (walked, rode, fell...), for "while moving" / "after standing still". */
public final class Movement {

    private record Seen(Vec3 pos, long movedAt) {}

    private static final Map<UUID, Seen> SEEN = new ConcurrentHashMap<>();

    private Movement() {}

    /** Called every player tick. */
    public static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        Seen seen = SEEN.get(player.getUUID());
        if (seen == null || seen.pos.distanceToSqr(player.position()) > 0.0025) {
            SEEN.put(player.getUUID(), new Seen(player.position(), seen == null ? now - 1_000_000 : now));
        }
    }

    /** Ticks since the player last moved. */
    public static long stillFor(ServerPlayer player) {
        Seen seen = SEEN.get(player.getUUID());
        return seen == null ? 0 : player.level().getGameTime() - seen.movedAt;
    }

    public static void forget(ServerPlayer player) {
        SEEN.remove(player.getUUID());
    }
}

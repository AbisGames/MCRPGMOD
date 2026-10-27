package com.pipican.ability;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Temporary helpers from {@code pipican:summon}. They are removed (no drops) when their time runs out,
 * when their owner leaves, and on server restart (they are tagged and cleaned up when they load again).
 */
public final class Summons {

    public static final String TAG = "pipican_summon";

    private record Entry(Mob mob, UUID owner, long endsAt) {}

    private static final Map<UUID, Entry> ACTIVE = new ConcurrentHashMap<>();

    private Summons() {}

    public static void track(Mob mob, ServerPlayer owner, long ticks) {
        mob.addTag(TAG);
        ACTIVE.put(mob.getUUID(), new Entry(mob, owner.getUUID(), owner.level().getGameTime() + ticks));
    }

    /** Called every server tick. */
    public static void tick(MinecraftServer server) {
        for (Iterator<Entry> it = ACTIVE.values().iterator(); it.hasNext(); ) {
            Entry e = it.next();
            boolean ownerGone = server.getPlayerList().getPlayer(e.owner()) == null;
            if (e.mob().isRemoved() || ownerGone || e.mob().level().getGameTime() >= e.endsAt()) {
                e.mob().discard();
                it.remove();
            }
        }
    }

    /** A tagged summon that loads but isn't tracked (e.g. after a restart) is left over: remove it. */
    public static boolean isLeftover(Entity entity) {
        return entity.entityTags().contains(TAG) && !ACTIVE.containsKey(entity.getUUID());
    }
}

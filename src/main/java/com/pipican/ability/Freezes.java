package com.pipican.ability;

import com.pipican.Pipican;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Mobs frozen in time ({@code pipican:freeze}, Time Stop): no AI, no movement, no gravity, they can't attack. Hits on
 * them still land. The mob remembers its own flags in its saved data, so a restart mid-freeze can't leave it frozen.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class Freezes {

    private static final String KEY = "pipican_frozen";
    private static final Map<Mob, Long> FROZEN = new ConcurrentHashMap<>();

    private Freezes() {}

    public static void freeze(Mob mob, long ticks) {
        long until = mob.level().getGameTime() + ticks;
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(KEY)) {
            CompoundTag saved = new CompoundTag();
            saved.putBoolean("no_ai", mob.isNoAi());
            saved.putBoolean("no_gravity", mob.isNoGravity());
            data.put(KEY, saved);
        }
        data.getCompoundOrEmpty(KEY).putLong("until", until);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setTarget(null);
        FROZEN.merge(mob, until, Math::max);
    }

    public static boolean isFrozen(Mob mob) {
        return FROZEN.containsKey(mob);
    }

    private static void thaw(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        CompoundTag saved = data.getCompoundOrEmpty(KEY);
        mob.setNoAi(saved.getBooleanOr("no_ai", false));
        mob.setNoGravity(saved.getBooleanOr("no_gravity", false));
        data.remove(KEY);
        FROZEN.remove(mob);
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (FROZEN.isEmpty()) {
            return;
        }
        for (var entry : FROZEN.entrySet()) {
            Mob mob = entry.getKey();
            if (!mob.isAlive() || mob.isRemoved() || mob.level().getGameTime() >= entry.getValue()) {
                thaw(mob);
            } else {
                mob.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    /** A mob saved while frozen comes back thawed. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Mob mob && mob.getPersistentData().contains(KEY)
                && !FROZEN.containsKey(mob)) {
            thaw(mob);
        }
    }

    /** Frozen mobs can't hurt anyone (projectiles they shot before still fly). */
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Mob mob && FROZEN.containsKey(mob)) {
            event.setCanceled(true);
        }
    }
}

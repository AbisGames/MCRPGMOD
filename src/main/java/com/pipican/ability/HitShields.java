package com.pipican.ability;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * "The next attack against them misses" from {@code pipican:negate_hit} (Twist Fate, Intercept): the
 * protected entity's next incoming hit within the time limit is cancelled, or - with a guard - lands on
 * the guard instead. Not saved.
 */
public final class HitShields {

    public record Shield(long endsAt, Optional<UUID> guard) {}

    private static final Map<UUID, Shield> SHIELDS = new ConcurrentHashMap<>();

    private HitShields() {}

    public static void add(LivingEntity protectedEntity, long ticks, Optional<UUID> guard) {
        SHIELDS.put(protectedEntity.getUUID(), new Shield(protectedEntity.level().getGameTime() + ticks, guard));
    }

    /** Takes (removes) a still-valid shield for this entity, if any. */
    public static Optional<Shield> take(LivingEntity entity) {
        Shield s = SHIELDS.remove(entity.getUUID());
        return s != null && s.endsAt() > entity.level().getGameTime() ? Optional.of(s) : Optional.empty();
    }
}

package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;

/**
 * {@code pipican:pin} - the reaction's mob can't teleport for {@code seconds} (endermen, shulkers, chorus fruit):
 * "unclean mobs you hit can't teleport for 5s".
 */
public record PinAction(double seconds) implements AbilityAction {

    public static final MapCodec<PinAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.05, 600).fieldOf("seconds").forGetter(PinAction::seconds)
    ).apply(i, PinAction::new));

    /** Entity -> game time its pin ends. */
    private static final Map<UUID, Long> PINNED = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.PIN;
    }

    @Override
    public void run(AbilityContext ctx) {
        long until = ctx.level().getGameTime() + Math.round((seconds + ctx.mod().seconds()) * 20);
        ctx.subject().ifPresent(e -> PINNED.put(e.getUUID(), until));
    }

    public static boolean pinned(Entity entity) {
        Long until = PINNED.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() >= until) {
            PINNED.remove(entity.getUUID());
            return false;
        }
        return true;
    }
}

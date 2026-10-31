package com.pipican.ability;

import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.PipicanAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Questions the mixins ask about a player's class bonuses (server side) or synced movement flags (both sides). */
public final class Trackless {

    private Trackless() {}

    /** {@code no_tripwires}: the player never sets off tripwires. */
    public static boolean ignoresTripwires(Entity entity) {
        return entity instanceof ServerPlayer p && ClassEffectApplier.state(p).chance(Kind.NO_TRIPWIRES) > 0;
    }

    /** {@code no_plates}: the player never presses pressure plates. */
    public static boolean ignoresPlates(Entity entity) {
        return entity instanceof ServerPlayer p && ClassEffectApplier.state(p).chance(Kind.NO_PLATES) > 0;
    }

    /** {@code unpushable}: the player and mobs don't push each other (synced, so the client agrees). */
    public static boolean unpushable(Entity entity) {
        return entity instanceof Player p && p.getData(PipicanAttachments.MOVE_FLAGS).has("unpushable");
    }

    /** {@code dry_riptide}: Riptide tridents work out of water and rain (synced, so the client agrees). */
    public static boolean dryRiptide(Player player) {
        return player.getData(PipicanAttachments.MOVE_FLAGS).has("dry_riptide");
    }

    /** {@code map_range}: maps the player carries reveal this much farther (0.25 = +25%). */
    public static double mapRange(Entity entity) {
        return entity instanceof ServerPlayer p ? ClassEffectApplier.state(p).chance(Kind.MAP_RANGE) : 0;
    }
}

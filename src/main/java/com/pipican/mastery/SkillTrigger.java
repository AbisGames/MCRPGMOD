package com.pipican.mastery;

import net.minecraft.server.level.ServerPlayer;
import com.mojang.serialization.Codec;

/**
 * Decides what counts as using a skill. Chosen in JSON by {@code "type"}, e.g.
 * <pre>{@code "trigger": { "type": "pipican:melee_hit", "items": "#pipican:weapons/swords" } }</pre>
 * Built-in types live in {@link SkillTriggerTypes}; adding one is a small class plus one line there.
 */
public interface SkillTrigger {

    Codec<SkillTrigger> CODEC = SkillTriggerTypes.TYPE_CODEC.dispatch("type", SkillTrigger::type, SkillTriggerTypes.Type::codec);

    SkillTriggerTypes.Type type();

    /** How many "uses" this action counts as (0 = doesn't match). Usually 0 or 1; movement gives fractions. */
    double uses(SkillAction action);

    /** Like {@link #uses(SkillAction)}, for triggers that also look at the player (situation filters). */
    default double uses(ServerPlayer player, SkillAction action) {
        return uses(action);
    }
}

package com.pipican.effect;

import com.mojang.serialization.Codec;

/**
 * One building block of a class: a passive bonus, a weakness, or the reward of a skill-tree node.
 *
 * <p>Every effect in JSON has a {@code "type"} field that selects its implementation, e.g.
 * <pre>{@code
 * { "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 2 }
 * { "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 }
 * }</pre>
 * The built-in types live in {@link ClassEffectTypes}. To add a brand-new kind of effect, write one
 * class implementing this interface and register it there - every class can then use it from JSON.
 *
 * <p>Recalculation runs in two passes because effects may scale with stats:
 * {@link #collectStats} first (only {@code pipican:stat} uses it), then {@link #apply}.
 */
public interface ClassEffect {

    Codec<ClassEffect> CODEC = ClassEffectTypes.TYPE_CODEC.dispatch("type", ClassEffect::type, ClassEffectTypes.Type::codec);

    ClassEffectTypes.Type type();

    /** Pass 1: contribute flat stat points. Most effects ignore this. */
    default void collectStats(EffectContext ctx) {}

    /** Pass 2: contribute attribute modifiers, potion effects, damage multipliers... */
    default void apply(EffectContext ctx) {}
}

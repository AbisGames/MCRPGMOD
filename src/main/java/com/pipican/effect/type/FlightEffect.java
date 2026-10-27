package com.pipican.effect.type;

import com.mojang.serialization.MapCodec;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;

/**
 * {@code pipican:flight} - creative-style flight while this effect counts (usually inside an ability
 * toggle or a {@code pipican:conditional}). Never touches creative or spectator players.
 *
 * <pre>{@code { "type": "pipican:flight" } }</pre>
 */
public record FlightEffect() implements ClassEffect {

    public static final MapCodec<FlightEffect> CODEC = MapCodec.unit(FlightEffect::new);

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.FLIGHT;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.grantFlight();
    }
}

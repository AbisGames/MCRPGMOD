package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;

/**
 * {@code pipican:weather} - starts rain (or a thunderstorm) for {@code seconds}, like {@code /weather rain}.
 *
 * <pre>{@code { "type": "pipican:weather", "seconds": 120, "thunder": false } }</pre>
 */
public record WeatherAction(double seconds, boolean thunder) implements AbilityAction {

    public static final MapCodec<WeatherAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 120.0).forGetter(WeatherAction::seconds),
            Codec.BOOL.optionalFieldOf("thunder", false).forGetter(WeatherAction::thunder)
    ).apply(i, WeatherAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.WEATHER;
    }

    @Override
    public void run(AbilityContext ctx) {
        // Weather is server-wide in this version, like /weather.
        ctx.level().getServer().setWeatherParameters(0, (int) (seconds * 20), true, thunder);
    }
}

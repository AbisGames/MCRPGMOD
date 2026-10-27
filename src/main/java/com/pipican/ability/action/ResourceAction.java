package com.pipican.ability.action;

import com.pipican.player.ClassEffectApplier;
import com.pipican.effect.type.BonusEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.player.PipicanAttachments;

/**
 * {@code pipican:resource} - refills (or drains, if negative) the user's resource bar by {@code amount}
 * plus {@code percent} of its max.
 *
 * <pre>{@code { "type": "pipican:resource", "percent": 0.5 } }</pre>
 */
public record ResourceAction(double amount, double percent) implements AbilityAction {

    public static final MapCodec<ResourceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(ResourceAction::amount),
            Codec.doubleRange(-1, 1).optionalFieldOf("percent", 0.0).forGetter(ResourceAction::percent)
    ).apply(i, ResourceAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RESOURCE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var data = ctx.player().getData(PipicanAttachments.CLASS_DATA);
        double change = amount + percent * data.resourceMax();
        if (change > 0) {
            change *= ClassEffectApplier.state(ctx.player()).bonus(BonusEffect.Kind.RESOURCE_GAIN);
        }
        data.setResource(Math.clamp(data.resource() + change, 0, data.resourceMax()));
    }
}

package com.pipican.mastery.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;

/**
 * {@code pipican:reaction} - one of your named reactions fired ("landing Trickery hits", "triggering Seize Chaos").
 *
 * <pre>{@code { "type": "pipican:reaction", "name": "trickery" } }</pre>
 */
public record ReactionTrigger(String name) implements SkillTrigger {

    public static final MapCodec<ReactionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ReactionTrigger::name)
    ).apply(i, ReactionTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.REACTION;
    }

    @Override
    public double uses(SkillAction action) {
        return action instanceof SkillAction.Reacted r && r.name().equalsIgnoreCase(name) ? 1 : 0;
    }
}

package com.pipican.mastery.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.MoveMode;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;

/**
 * {@code pipican:move} - travelling in a movement mode. Every {@code per_blocks} blocks counts as
 * one use (fractions accumulate). Not limited by the skill cooldown.
 *
 * <pre>{@code { "type": "pipican:move", "mode": "sprint", "per_blocks": 20 } }</pre>
 * Modes: walk, sprint, sneak, swim, climb, glide.
 */
public record MoveTrigger(MoveMode mode, double perBlocks) implements SkillTrigger {

    public static final MapCodec<MoveTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MoveMode.CODEC.fieldOf("mode").forGetter(MoveTrigger::mode),
            Codec.doubleRange(0.01, 100_000).optionalFieldOf("per_blocks", 10.0).forGetter(MoveTrigger::perBlocks)
    ).apply(i, MoveTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.MOVE;
    }

    @Override
    public double uses(SkillAction action) {
        if (action instanceof SkillAction.Moved moved && moved.mode() == mode) {
            return moved.distance() / perBlocks;
        }
        return 0;
    }
}

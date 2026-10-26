package com.pipican.mastery.trigger;

import com.mojang.serialization.MapCodec;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;

/** {@code pipican:jump} - every jump is one use. {@code { "type": "pipican:jump" }} */
public record JumpTrigger() implements SkillTrigger {

    public static final MapCodec<JumpTrigger> CODEC = MapCodec.unit(new JumpTrigger());

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.JUMP;
    }

    @Override
    public double uses(SkillAction action) {
        return action instanceof SkillAction.Jumped ? 1 : 0;
    }
}

package com.pipican.mastery.trigger;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;
import net.minecraft.resources.Identifier;

/**
 * {@code pipican:ability} - an active ability (dodge roll, fireball...) was used.
 *
 * <pre>{@code { "type": "pipican:ability", "ability": "pipican:dodge_roll" } }</pre>
 * Nothing fires this yet: the ability system (keybinds) will call
 * {@code MasteryService.onAction(player, new SkillAction.AbilityUsed(id, item))}.
 */
public record AbilityTrigger(Identifier ability) implements SkillTrigger {

    public static final MapCodec<AbilityTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("ability").forGetter(AbilityTrigger::ability)
    ).apply(i, AbilityTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.ABILITY;
    }

    @Override
    public double uses(SkillAction action) {
        return action instanceof SkillAction.AbilityUsed used && used.ability().equals(ability) ? 1 : 0;
    }
}

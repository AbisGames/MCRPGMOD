package com.pipican.mastery.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:any} - a skill trained by several things ("brewing potions and throwing them at mobs").
 *
 * <pre>{@code { "type": "pipican:any", "triggers": [ { "type": "pipican:did", "action": "brew" }, { "type": "pipican:kill" } ] } }</pre>
 * The first trigger that matches counts.
 */
public record AnyTrigger(List<SkillTrigger> triggers) implements SkillTrigger {

    public static final MapCodec<AnyTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.lazyInitialized(() -> SkillTrigger.CODEC).listOf().fieldOf("triggers").forGetter(AnyTrigger::triggers)
    ).apply(i, AnyTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.ANY;
    }

    @Override
    public double uses(SkillAction action) {
        for (SkillTrigger t : triggers) {
            double uses = t.uses(action);
            if (uses > 0) {
                return uses;
            }
        }
        return 0;
    }

    @Override
    public double uses(ServerPlayer player, SkillAction action) {
        for (SkillTrigger t : triggers) {
            double uses = t.uses(player, action);
            if (uses > 0) {
                return uses;
            }
        }
        return 0;
    }
}

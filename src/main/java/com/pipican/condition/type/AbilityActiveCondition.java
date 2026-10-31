package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Ability;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import com.pipican.core.PipicanRegistries;
import com.pipican.player.PipicanAttachments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:ability_active} - one of the player's ability states (a timed state or a toggle) is running.
 *
 * <pre>{@code { "type": "pipican:ability_active", "ability": "pipican:champion/battle_stance" } }</pre>
 */
public record AbilityActiveCondition(ResourceKey<Ability> ability) implements ClassCondition {

    public static final MapCodec<AbilityActiveCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.ABILITY).fieldOf("ability").forGetter(AbilityActiveCondition::ability)
    ).apply(i, AbilityActiveCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.ABILITY_ACTIVE;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.getData(PipicanAttachments.CLASS_DATA).abilityActive().containsKey(ability);
    }
}

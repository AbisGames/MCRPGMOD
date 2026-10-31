package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Flags;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:flag} - a {@code pipican:flag} timer called {@code name} is running on the player.
 *
 * <pre>{@code { "type": "pipican:flag", "name": "berserk lockout" } }</pre>
 */
public record FlagCondition(String name) implements ClassCondition {

    public static final MapCodec<FlagCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(FlagCondition::name)
    ).apply(i, FlagCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.FLAG;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return Flags.has(player, name);
    }
}

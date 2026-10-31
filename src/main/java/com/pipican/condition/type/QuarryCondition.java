package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Quarries;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:quarry} - the player has a live {@code pipican:quarry} called {@code name} ("while a mark is active").
 *
 * <pre>{@code { "type": "pipican:quarry", "name": "vow of vengeance" } }</pre>
 */
public record QuarryCondition(String name) implements ClassCondition {

    public static final MapCodec<QuarryCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(QuarryCondition::name)
    ).apply(i, QuarryCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.QUARRY;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return Quarries.has(player, name.toLowerCase(java.util.Locale.ROOT));
    }
}

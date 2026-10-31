package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import com.pipican.player.ClassEffectApplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:special_stat} - the class's special stat is at least {@code min} ("per 10 Staffcraft, ..." from 10 on).
 *
 * <pre>{@code { "type": "pipican:special_stat", "min": 10 } }</pre>
 */
public record SpecialStatCondition(double min) implements ClassCondition {

    public static final MapCodec<SpecialStatCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("min").forGetter(SpecialStatCondition::min)
    ).apply(i, SpecialStatCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.SPECIAL_STAT;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return ClassEffectApplier.sourceValue(player, "special_stat") >= min;
    }
}

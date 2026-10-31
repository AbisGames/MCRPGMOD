package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import com.pipican.effect.type.BonusEffect;
import com.pipican.player.ClassEffectApplier;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:temple} - the player is within the radius of their temple: their respawn point (a bed or respawn
 * anchor they set) in the same dimension. The radius is 10 blocks plus {@code temple_radius} bonuses, or {@code radius}
 * when given ("more than 100 blocks from your temple" = {@code not} with radius 100). A temple that follows its player
 * ({@code temple_mobile}) always holds; see {@link com.pipican.ability.Temples}.
 */
public record TempleCondition(Optional<Double> radius) implements ClassCondition {

    public static final MapCodec<TempleCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 100_000).optionalFieldOf("radius").forGetter(TempleCondition::radius)
    ).apply(i, TempleCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.TEMPLE;
    }

    @Override
    public boolean test(ServerPlayer player) {
        // A temple that follows its player is always around them, however far "away" counts.
        if (com.pipican.ability.Temples.mobile(player)) {
            return true;
        }
        return com.pipican.ability.Temples.contains(player, player.position(), radius.orElseGet(() -> com.pipican.ability.Temples.radius(player)));
    }
}

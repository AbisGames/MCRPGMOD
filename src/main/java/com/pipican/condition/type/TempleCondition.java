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
 * when given ("more than 100 blocks from your temple" = {@code not} with radius 100).
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
        ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
        if (respawn == null || !respawn.respawnData().globalPos().dimension().equals(player.level().dimension())) {
            return false;
        }
        // The radius bonus comes from the bonuses computed last time (they rarely change).
        double r = radius.orElseGet(() -> 10 + ClassEffectApplier.state(player).chance(BonusEffect.Kind.TEMPLE_RADIUS));
        return respawn.respawnData().globalPos().pos().getCenter().distanceToSqr(player.position()) <= r * r;
    }
}

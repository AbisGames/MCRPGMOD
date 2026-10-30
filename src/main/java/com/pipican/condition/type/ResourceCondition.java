package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.pipican.condition.Bounds;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:resource} - the class resource bar (mana, rage...) as a fraction of its max (0-1).
 * False for classes without a resource.
 *
 * <pre>{@code { "type": "pipican:resource", "min": 1 } }   bar is full</pre>
 * With {@code lowerable}, {@code threshold_drop} bonuses lower {@code min}.
 */
public record ResourceCondition(Bounds bounds, boolean lowerable) implements ClassCondition {

    public static final MapCodec<ResourceCondition> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            Bounds.MAP_CODEC.forGetter(ResourceCondition::bounds),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("lowerable", false).forGetter(ResourceCondition::lowerable)
    ).apply(i, ResourceCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.RESOURCE;
    }

    @Override
    public boolean test(ServerPlayer player) {
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        if (data.resourceMax() <= 0) {
            return false;
        }
        double fraction = data.resource() / data.resourceMax();
        // "Berserk triggers at 50% Rage instead of 75%": threshold_drop lowers a lowerable minimum.
        if (lowerable && bounds.min().isPresent()) {
            double drop = com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.THRESHOLD_DROP);
            return fraction >= bounds.min().get() - drop && (bounds.max().isEmpty() || fraction <= bounds.max().get());
        }
        return bounds.test(fraction);
    }
}

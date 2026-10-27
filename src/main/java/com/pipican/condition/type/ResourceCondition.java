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
 */
public record ResourceCondition(Bounds bounds) implements ClassCondition {

    public static final MapCodec<ResourceCondition> CODEC = Bounds.MAP_CODEC.xmap(ResourceCondition::new, ResourceCondition::bounds);

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.RESOURCE;
    }

    @Override
    public boolean test(ServerPlayer player) {
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        return data.resourceMax() > 0 && bounds.test(data.resource() / data.resourceMax());
    }
}

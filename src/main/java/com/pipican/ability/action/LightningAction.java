package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:lightning} - calls a lightning bolt on the mob in the crosshair, or on the block looked at.
 * Never aims at players.
 *
 * <pre>{@code { "type": "pipican:lightning", "range": 32 } }</pre>
 */
public record LightningAction(double range) implements AbilityAction {

    public static final MapCodec<LightningAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 128).optionalFieldOf("range", 32.0).forGetter(LightningAction::range)
    ).apply(i, LightningAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.LIGHTNING;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        // In a reaction ("your hits may call down lightning") strike the mob that was hit.
        var target = ctx.subject().filter(e -> !(e instanceof Player)).orElseGet(() -> Targets.lookTarget(player, range));
        Vec3 at;
        if (target != null && !(target instanceof Player)) {
            at = target.position();
        } else {
            HitResult hit = player.pick(range, 1.0f, false);
            if (hit.getType() == HitResult.Type.MISS) {
                return;
            }
            at = hit.getLocation();
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ctx.level(), EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(at);
            bolt.setCause(player);
            ctx.level().addFreshEntity(bolt);
        }
    }
}

package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:break_free} - frees the user from whatever holds them (Escape): removes Slowness, breaks the cobwebs,
 * sweet berry bushes and powder snow they're stuck in, snaps guardians' beams locked on them, cuts leads, gets them off
 * a mob that's carrying them, and hops them free. For {@code seconds} afterwards Slowness can't take hold again.
 *
 * <pre>{@code { "type": "pipican:break_free", "seconds": 3 } }</pre>
 */
public record BreakFreeAction(double seconds) implements AbilityAction {

    public static final MapCodec<BreakFreeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 60).optionalFieldOf("seconds", 3.0).forGetter(BreakFreeAction::seconds)
    ).apply(i, BreakFreeAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BREAK_FREE;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        player.removeEffect(MobEffects.SLOWNESS);
        // Stuck in blocks.
        BlockPos.betweenClosedStream(player.getBoundingBox().inflate(0.1)).forEach(pos -> {
            var state = ctx.level().getBlockState(pos);
            if (state.is(Blocks.COBWEB) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
                ctx.level().destroyBlock(pos, true, player);
            }
        });
        // Held by mobs: guardian beams, leads, being carried.
        for (Guardian guardian : ctx.level().getEntitiesOfClass(Guardian.class, player.getBoundingBox().inflate(16), g -> g.getTarget() == player)) {
            guardian.setTarget(null);
        }
        for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12), m -> m.getLeashHolder() == player)) {
            mob.dropLeash();
        }
        if (player.getVehicle() instanceof Mob carrier && !(carrier instanceof net.minecraft.world.entity.animal.equine.AbstractHorse)) {
            player.stopRiding();
        }
        player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, 0.4, player.getDeltaMovement().z));
        player.hurtMarked = true;
        if (seconds > 0) {
            com.pipican.ability.Boosts.add(player, this, java.util.List.of(new com.pipican.effect.type.ImmuneEffect(
                    net.minecraft.core.HolderSet.direct(MobEffects.SLOWNESS))), (int) Math.round(seconds * 20), 1);
        }
    }
}

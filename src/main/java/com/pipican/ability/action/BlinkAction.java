package com.pipican.ability.action;

import com.pipican.mastery.SkillAction;
import com.pipican.event.MasteryEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:blink} - teleports the user up to {@code distance} blocks where they look, stopping short of walls.
 *
 * <pre>{@code { "type": "pipican:blink", "distance": 8, "through_walls": false, "require_blocks": "#minecraft:logs" } }</pre>
 * {@code through_walls}: go through solid blocks to the farthest free spot in range.
 * {@code require_blocks}: only works when looking at one of these blocks, and arrives beside it
 * (otherwise the use is cancelled and refunded).
 * {@code max_light}: the arrival spot must be this dark or darker ("teleport between shadows").
 * {@code start_near}: the user must be touching one of these blocks (within 1.5 blocks) to set off ("step into a log").
 */
public record BlinkAction(double distance, boolean throughWalls, Optional<HolderSet<Block>> requireBlocks, int maxLight,
                          Optional<HolderSet<Block>> startNear) implements AbilityAction {

    public static final MapCodec<BlinkAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 64).optionalFieldOf("distance", 8.0).forGetter(BlinkAction::distance),
            Codec.BOOL.optionalFieldOf("through_walls", false).forGetter(BlinkAction::throughWalls),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("require_blocks").forGetter(BlinkAction::requireBlocks),
            Codec.intRange(0, 15).optionalFieldOf("max_light", 15).forGetter(BlinkAction::maxLight),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("start_near").forGetter(BlinkAction::startNear)
    ).apply(i, BlinkAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BLINK;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        // Upgrades ("+1 blink distance") come as a reaction upgrade's radius or an ability upgrade's radius.
        double distance = this.distance + ctx.mod().radius()
                + ctx.ability().map(k -> com.pipican.player.ClassEffectApplier.state(player).abilityMod(k).radius()).orElse(0.0);
        Vec3 look = player.getLookAngle();
        if (startNear.isPresent() && BlockPos.betweenClosedStream(player.getBoundingBox().inflate(1.5)).noneMatch(p -> ctx.level().getBlockState(p).is(startNear.get()))) {
            ctx.cancel(Component.translatable("pipican.ability.needs_start_block"));
            return;
        }
        LIGHT.set(maxLight);
        try {
            go(ctx, player, distance, look);
        } finally {
            LIGHT.set(15);
        }
    }

    private static final ThreadLocal<Integer> LIGHT = ThreadLocal.withInitial(() -> 15);

    private void go(AbilityContext ctx, ServerPlayer player, double distance, Vec3 look) {
        HitResult hit = player.pick(distance, 1.0f, false);
        if (requireBlocks.isPresent()) {
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS
                    || !ctx.level().getBlockState(blockHit.getBlockPos()).is(requireBlocks.get())) {
                ctx.cancel(Component.translatable("pipican.ability.needs_block"));
                return;
            }
            BlockPos side = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (!tryAt(ctx, Vec3.atBottomCenterOf(side))) {
                ctx.cancel(Component.translatable("pipican.ability.no_room"));
            }
            return;
        }
        if (throughWalls) {
            for (double d = distance; d >= 1; d -= 0.5) {
                Vec3 feet = player.getEyePosition().add(look.scale(d)).subtract(0, player.getEyeHeight(), 0);
                if (tryAt(ctx, feet)) {
                    return;
                }
            }
            ctx.cancel(Component.translatable("pipican.ability.no_room"));
            return;
        }
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(look.scale(distance))
                : hit.getLocation().subtract(look.scale(0.8));
        if (LIGHT.get() < 15) {
            // Between shadows: the farthest dark enough spot along the way.
            Vec3 eye = player.getEyePosition();
            double reach = Math.min(distance, eye.distanceTo(target));
            for (double d = reach; d >= 1; d -= 0.5) {
                if (tryAt(ctx, eye.add(look.scale(d)).subtract(0, player.getEyeHeight(), 0))) {
                    return;
                }
            }
            ctx.cancel(Component.translatable("pipican.ability.no_shadow"));
            return;
        }
        if (!tryAt(ctx, target.subtract(0, player.getEyeHeight(), 0))) {
            ctx.cancel(Component.translatable("pipican.ability.no_room"));
        }
    }

    /** Teleports the player's feet to {@code feet}, nudged up to 3 blocks until they fit. */
    static boolean tryAt(AbilityContext ctx, Vec3 feet) {
        ServerPlayer player = ctx.player();
        for (int i = 0; i < 4; i++) {
            if (ctx.level().noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())))
                    && ctx.level().getMaxLocalRawBrightness(BlockPos.containing(feet)) <= LIGHT.get()) {
                player.teleportTo(feet.x, feet.y, feet.z);
                player.resetFallDistance();
                MasteryEvents.did(player, SkillAction.Did.Kind.TELEPORT);
                return true;
            }
            feet = feet.add(0, 1, 0);
        }
        return false;
    }
}

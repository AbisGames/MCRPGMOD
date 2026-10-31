package com.pipican.client;

import com.pipican.player.PipicanAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Client side of movement abilities (the client moves its own player): double jump, wall run, wall climb, glide, no cobweb slowdown. */
final class MovementClient {

    private static boolean jumpWasDown;
    private static boolean airJumpUsed;
    private static long lastAirJump = -1000;
    private static double groundY;

    private MovementClient() {}

    /** Hide Your Name: other players' names stay hidden while they sneak or are invisible. */
    static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player other && other != Minecraft.getInstance().player
                && other.getData(PipicanAttachments.MOVE_FLAGS).has("hide_nameplate") && (other.isCrouching() || other.isInvisible())) {
            event.setCanRender(net.minecraft.util.TriState.FALSE);
        }
    }

    private static long lastSteelPush;
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> METAL_BLOCKS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, com.pipican.Pipican.id("allomancy/metal_blocks"));

    private static boolean metalBelow(net.minecraft.world.level.Level level, BlockPos feet) {
        for (int d = 1; d <= 5; d++) {
            if (level.getBlockState(feet.below(d)).is(METAL_BLOCKS)) {
                return true;
            }
        }
        return false;
    }

    static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || player != Minecraft.getInstance().player) {
            return;
        }
        var flags = player.getData(PipicanAttachments.MOVE_FLAGS);
        if (flags.flags().isEmpty()) {
            return;
        }
        boolean jump = Minecraft.getInstance().options.keyJump.isDown();
        boolean pressed = jump && !jumpWasDown;
        jumpWasDown = jump;
        if (player.getAbilities().flying || player.isPassenger() || player.isFallFlying()) {
            return;
        }
        // Sinking: in water you can't rise, swimming up or not.
        if (flags.has("sink") && player.isInWater()) {
            Vec3 down = player.getDeltaMovement();
            player.setDeltaMovement(down.x, Math.min(down.y, -0.06), down.z);
        }
        // Cobwebs / powder snow: drop the slowdown they set this tick.
        BlockPos feet = player.blockPosition();
        var level = player.level();
        boolean web = level.getBlockState(feet).is(Blocks.COBWEB) || level.getBlockState(feet.above()).is(Blocks.COBWEB);
        boolean snow = level.getBlockState(feet).is(Blocks.POWDER_SNOW) || level.getBlockState(feet.above()).is(Blocks.POWDER_SNOW);
        if (web && flags.has("no_cobweb") || snow && flags.has("no_powder_snow")) {
            player.stuckSpeedMultiplier = Vec3.ZERO;
        }
        Vec3 v = player.getDeltaMovement();
        if (player.onGround()) {
            airJumpUsed = false;
            groundY = player.getY();
            return;
        }
        if (player.isInWater() || player.onClimbable()) {
            return;
        }
        long now = level.getGameTime();
        // Steel (Allomancy): jump in mid-air with metal up to 5 blocks below to push off it.
        if (pressed && flags.has("steel_push") && now - lastSteelPush >= 20 && metalBelow(level, player.blockPosition())) {
            player.setDeltaMovement(v.x, 1.0, v.z);
            lastSteelPush = now;
            return;
        }
        if (pressed && flags.has("double_jump") && !airJumpUsed && now - lastAirJump >= 100) {
            player.setDeltaMovement(v.x, 0.42 + player.getJumpBoostPower(), v.z);
            airJumpUsed = true;
            lastAirJump = now;
            return;
        }
        if (player.horizontalCollision) {
            if (flags.has("wall_climb") && player.isShiftKeyDown() && jump) {
                player.setDeltaMovement(v.x, 0.2, v.z);
                return;
            }
            if (flags.has("wall_run") && player.isSprinting() && player.getY() - groundY < 2) {
                player.setDeltaMovement(v.x, 0.3, v.z);
                return;
            }
        }
        if (flags.has("glide") && jump && v.y < -0.08) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 0.6, Math.max(v.y, -0.12), look.z * 0.6);
            player.resetFallDistance();
        }
    }
}

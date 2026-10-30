package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * {@code pipican:state} - a simple yes/no fact about the player or their surroundings.
 *
 * <pre>{@code { "type": "pipican:state", "state": "wet" } }</pre>
 */
public record StateCondition(State state) implements ClassCondition {

    public enum State implements StringRepresentable {
        /** In water or standing in rain. */
        WET("wet"),
        IN_WATER("in_water"),
        UNDERWATER("underwater"),
        IN_LAVA("in_lava"),
        SWIMMING("swimming"),
        SPRINTING("sprinting"),
        SNEAKING("sneaking"),
        /** Holding up a shield. */
        BLOCKING("blocking"),
        MOUNTED("mounted"),
        /** Standing on solid ground (not falling, flying or swimming). */
        ON_GROUND("on_ground"),
        /** Hit, or was hit by, a mob or player within the last 5 seconds. */
        IN_COMBAT("in_combat"),
        ON_FIRE("on_fire"),
        /** Night in a dimension with a day cycle (never true in the Nether or the End). */
        NIGHT("night"),
        /** Daytime in a dimension with a day cycle. */
        DAY("day"),
        /** Rain (or snow) is falling on the player. */
        RAINING("raining"),
        /** A thunderstorm, and rain is falling on the player. */
        THUNDERING("thundering"),
        /** Nothing but transparent blocks above the player's head. "Underground" is {@code not} this. */
        SEES_SKY("sees_sky"),
        /** Inside a village (near villagers' beds and workstations). */
        IN_VILLAGE("in_village"),
        /** Within 64 blocks of your respawn point (bed / anchor, else world spawn). */
        NEAR_HOME("near_home"),
        /** You have a live mark (curse, scrutiny, vow...) on some mob. */
        MARKING("marking"),
        /** The first 6 seconds of a fight ("first round of combat"). */
        COMBAT_START("combat_start"),
        /** Standing in a 1-block-wide space: solid blocks on both sides along one axis. */
        NARROW("narrow"),
        /** Standing in leaves, tall grass or ferns. */
        IN_FOLIAGE("in_foliage");

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);
        private final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<StateCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            State.CODEC.fieldOf("state").forGetter(StateCondition::state)
    ).apply(i, StateCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.STATE;
    }

    private static boolean foliage(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS) || state.is(net.minecraft.world.level.block.Blocks.FERN)
                || state.is(net.minecraft.world.level.block.Blocks.LARGE_FERN);
    }

    private static boolean narrow(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        var level = player.level();
        java.util.function.Predicate<BlockPos> solid = p -> level.getBlockState(p).isCollisionShapeFullBlock(level, p);
        return solid.test(feet.east()) && solid.test(feet.west()) || solid.test(feet.north()) && solid.test(feet.south());
    }

    private static boolean nearHome(ServerPlayer player) {
        var config = player.getRespawnConfig();
        var home = config != null ? config.respawnData() : player.level().getRespawnData();
        return home.dimension() == player.level().dimension() && home.pos().distSqr(player.blockPosition()) <= 64 * 64;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var level = player.level();
        return switch (state) {
            case WET -> player.isInWaterOrRain();
            case IN_WATER -> player.isInWater();
            case UNDERWATER -> player.isUnderWater();
            case IN_LAVA -> player.isInLava();
            case SWIMMING -> player.isSwimming();
            case SPRINTING -> player.isSprinting();
            case SNEAKING -> player.isShiftKeyDown();
            case BLOCKING -> player.isBlocking();
            case MOUNTED -> player.isPassenger();
            case ON_GROUND -> player.onGround();
            case IN_COMBAT -> player instanceof net.minecraft.server.level.ServerPlayer sp && com.pipican.ability.Combat.inCombat(sp);
            case ON_FIRE -> player.isOnFire();
            case NIGHT -> level.isDarkOutside();
            case DAY -> !level.dimensionType().hasFixedTime() && level.isBrightOutside();
            case RAINING -> level.isRainingAt(player.blockPosition());
            case THUNDERING -> level.isThundering() && level.isRainingAt(player.blockPosition());
            case SEES_SKY -> level.canSeeSky(BlockPos.containing(player.getEyePosition()));
            case IN_VILLAGE -> level instanceof net.minecraft.server.level.ServerLevel sl && sl.isVillage(player.blockPosition());
            case NEAR_HOME -> nearHome(player);
            case MARKING -> com.pipican.ability.Marks.hasAny(player);
            case COMBAT_START -> {
                long t = com.pipican.ability.Combat.fightTicks(player);
                yield t >= 0 && t < 120;
            }
            case NARROW -> narrow(player);
            case IN_FOLIAGE -> foliage(level.getBlockState(player.blockPosition())) || foliage(level.getBlockState(BlockPos.containing(player.getEyePosition())));
        };
    }
}

package com.pipican.mastery;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

/** Kinds of movement tracked for {@code pipican:move} skills. Each tick counts as exactly one mode. */
public enum MoveMode implements StringRepresentable {
    WALK("walk"),
    SPRINT("sprint"),
    SNEAK("sneak"),
    SWIM("swim"),
    CLIMB("climb"),
    GLIDE("glide");

    public static final Codec<MoveMode> CODEC = StringRepresentable.fromEnum(MoveMode::values);

    private final String name;

    MoveMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The mode the player is moving in right now, checked from most to least specific. */
    public static MoveMode of(Player player) {
        if (player.isFallFlying()) {
            return GLIDE;
        }
        if (player.isSwimming() || player.isInWater()) {
            return SWIM;
        }
        if (player.onClimbable()) {
            return CLIMB;
        }
        if (player.isCrouching()) {
            return SNEAK;
        }
        return player.isSprinting() ? SPRINT : WALK;
    }
}

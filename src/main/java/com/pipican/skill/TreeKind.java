package com.pipican.skill;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Which of a class's three trees a node belongs to. Each kind has its own pool of points:
 * {@link #GENERAL} and {@link #SPECIAL} are paid with skill points from mastery, {@link #STAT}
 * with stat points from kill XP (the same points used to raise stats directly).
 */
public enum TreeKind implements StringRepresentable {
    GENERAL("general"),
    SPECIAL("special"),
    STAT("stat");

    public static final Codec<TreeKind> CODEC = StringRepresentable.fromEnum(TreeKind::values);

    private final String name;

    TreeKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

package com.pipican.skill;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Which of a class's two trees a node belongs to. Each kind has its own pool of skill points. */
public enum TreeKind implements StringRepresentable {
    GENERAL("general"),
    SPECIAL("special");

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

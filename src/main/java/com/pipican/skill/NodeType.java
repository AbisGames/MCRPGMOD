package com.pipican.skill;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Size/role of a passive-tree node, Path of Exile style. Only affects how the node is drawn and
 * whether it can be bought; what it does is entirely up to its effects.
 */
public enum NodeType implements StringRepresentable {
    /** Minor bonus. */
    SMALL("small", 18),
    /** Strong, named bonus. */
    NOTABLE("notable", 24),
    /** Build-defining bonus with a real downside. */
    KEYSTONE("keystone", 30),
    /** A class's starting point: owned for free, never bought or refunded. */
    START("start", 28);

    public static final Codec<NodeType> CODEC = StringRepresentable.fromEnum(NodeType::values);

    private final String name;
    private final int size;

    NodeType(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Drawn size in tree-screen pixels at zoom 1. */
    public int size() {
        return size;
    }
}

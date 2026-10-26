package com.pipican.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * A class's place in a tree: which tree, and which node it starts from.
 *
 * <pre>{@code "skill_tree": { "tree": "pipican:main", "start": "warrior_start" } }</pre>
 * Several classes can share one tree with different start nodes (Path of Exile style).
 */
public record TreeRef(Identifier tree, String start) {

    public static final Codec<TreeRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("tree").forGetter(TreeRef::tree),
            Codec.STRING.fieldOf("start").forGetter(TreeRef::start)
    ).apply(i, TreeRef::new));
}

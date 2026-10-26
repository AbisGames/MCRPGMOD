package com.pipican.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

/**
 * One JSON file of a passive tree, at {@code data/<ns>/pipican/skill_tree/<name>.json}.
 *
 * <p>A big tree can be split into many files (e.g. one per region) so several people can work on it
 * at once: every file with the same {@code "tree"} value is merged into one {@link SkillTree}.
 * Without {@code "tree"}, the file is a tree on its own whose id is the file's id.
 *
 * <pre>{@code
 * { "tree": "pipican:main", "name": "Passive Tree", "nodes": [ ...SkillNode... ] }
 * }</pre>
 *
 * @param tree  id of the tree this file belongs to (defaults to this file's id)
 * @param name  display name of the tree; only one file per tree needs it
 * @param nodes the nodes in this file
 */
public record SkillTreeFile(Optional<Identifier> tree, Optional<Component> name, List<SkillNode> nodes) {

    public static final Codec<SkillTreeFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("tree").forGetter(SkillTreeFile::tree),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(SkillTreeFile::name),
            SkillNode.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(SkillTreeFile::nodes)
    ).apply(i, SkillTreeFile::new));
}

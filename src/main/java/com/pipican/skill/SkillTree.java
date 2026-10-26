package com.pipican.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A skill tree, loaded from {@code data/<ns>/pipican/skill_tree/<name>.json}.
 *
 * <p>Classes reference trees by id ({@code skill_tree} and {@code special_skill_tree} in the class
 * JSON), so a tree can in principle be shared between classes. Each class normally has:
 * <ul>
 *   <li>a <b>general</b> tree - its own take on common upgrades, paid with general skill points</li>
 *   <li>a <b>special</b> tree - its signature tree around the special stat, paid with special points</li>
 * </ul>
 * Both kinds use this same format.
 *
 * <pre>{@code { "name": "Warrior Arts", "nodes": [ ...SkillNode... ] } }</pre>
 */
public record SkillTree(Component name, List<SkillNode> nodes) {

    public static final Codec<SkillTree> CODEC = RecordCodecBuilder.<SkillTree>create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillTree::name),
            SkillNode.CODEC.listOf().fieldOf("nodes").forGetter(SkillTree::nodes)
    ).apply(i, SkillTree::new)).validate(SkillTree::validate);

    public Optional<SkillNode> node(String id) {
        for (SkillNode node : nodes) {
            if (node.id().equals(id)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /** Catches typos at load time: duplicate ids or "requires" pointing at missing nodes. */
    private static DataResult<SkillTree> validate(SkillTree tree) {
        Map<String, SkillNode> byId = new LinkedHashMap<>();
        for (SkillNode node : tree.nodes) {
            if (byId.put(node.id(), node) != null) {
                return DataResult.error(() -> "Duplicate skill node id '" + node.id() + "'");
            }
        }
        Set<String> missing = new HashSet<>();
        for (SkillNode node : tree.nodes) {
            for (String req : node.requires()) {
                if (!byId.containsKey(req)) {
                    missing.add(node.id() + " -> " + req);
                }
            }
        }
        if (!missing.isEmpty()) {
            return DataResult.error(() -> "Skill nodes require unknown nodes: " + missing);
        }
        return DataResult.success(tree);
    }
}

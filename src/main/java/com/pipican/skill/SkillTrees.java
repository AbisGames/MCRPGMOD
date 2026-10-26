package com.pipican.skill;

import com.pipican.Pipican;
import com.pipican.core.PipicanRegistries;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Merges {@link SkillTreeFile}s into {@link SkillTree}s. The result is cached per registry
 * instance (registries are fixed for the lifetime of a world/connection), on server and client.
 *
 * <p>Problems in the data (duplicate node ids, links to missing nodes) are logged and skipped
 * instead of crashing, because they can span files written by different people.
 */
public final class SkillTrees {

    private static final Map<Registry<SkillTreeFile>, Map<Identifier, SkillTree>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private SkillTrees() {}

    public static Optional<SkillTree> get(RegistryAccess access, Identifier treeId) {
        Registry<SkillTreeFile> files = access.lookupOrThrow(PipicanRegistries.SKILL_TREE);
        return Optional.ofNullable(CACHE.computeIfAbsent(files, SkillTrees::assemble).get(treeId));
    }

    private static Map<Identifier, SkillTree> assemble(Registry<SkillTreeFile> files) {
        // Group files by tree id, in a stable order.
        Map<Identifier, Map<String, SkillNode>> nodesByTree = new LinkedHashMap<>();
        Map<Identifier, Component> names = new HashMap<>();
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((a, b) -> a.identifier().compareTo(b.identifier())))
                .forEach(entry -> {
                    ResourceKey<SkillTreeFile> fileKey = entry.getKey();
                    SkillTreeFile file = entry.getValue();
                    Identifier treeId = file.tree().orElse(fileKey.identifier());
                    file.name().ifPresent(name -> names.putIfAbsent(treeId, name));
                    Map<String, SkillNode> nodes = nodesByTree.computeIfAbsent(treeId, k -> new LinkedHashMap<>());
                    for (SkillNode node : file.nodes()) {
                        if (nodes.putIfAbsent(node.id(), node) != null) {
                            Pipican.LOGGER.error("Skill tree {}: duplicate node '{}' in {} (ignored)", treeId, node.id(), fileKey.identifier());
                        }
                    }
                });

        Map<Identifier, SkillTree> trees = new HashMap<>();
        nodesByTree.forEach((treeId, nodes) -> {
            Map<String, Set<String>> neighbours = new HashMap<>();
            for (SkillNode node : nodes.values()) {
                for (String link : node.links()) {
                    if (!nodes.containsKey(link)) {
                        Pipican.LOGGER.error("Skill tree {}: node '{}' links to unknown node '{}' (ignored)", treeId, node.id(), link);
                        continue;
                    }
                    neighbours.computeIfAbsent(node.id(), k -> new HashSet<>()).add(link);
                    neighbours.computeIfAbsent(link, k -> new HashSet<>()).add(node.id());
                }
            }
            Component name = names.getOrDefault(treeId, Component.literal(treeId.toString()));
            trees.put(treeId, new SkillTree(treeId, name, nodes, neighbours));
        });
        return trees;
    }
}

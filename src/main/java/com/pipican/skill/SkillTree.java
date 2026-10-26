package com.pipican.skill;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A fully assembled passive tree: all its {@link SkillTreeFile}s merged, with links made two-way.
 * Built by {@link SkillTrees}; never loaded from JSON directly.
 *
 * <p>Allocation rules (Path of Exile style) live here so server and client agree:
 * a node can be allocated when it touches an owned node (or the class's start node), and refunded
 * only if every other owned node is still connected to the start afterwards.
 */
public final class SkillTree {

    private final Identifier id;
    private final Component name;
    private final Map<String, SkillNode> nodes;
    private final Map<String, Set<String>> neighbours;

    SkillTree(Identifier id, Component name, Map<String, SkillNode> nodes, Map<String, Set<String>> neighbours) {
        this.id = id;
        this.name = name;
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        this.neighbours = neighbours;
    }

    public Identifier id() {
        return id;
    }

    public Component name() {
        return name;
    }

    public Collection<SkillNode> nodes() {
        return nodes.values();
    }

    public Optional<SkillNode> node(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Set<String> neighbours(String id) {
        return neighbours.getOrDefault(id, Set.of());
    }

    /** True if {@code node} touches the start node or any owned node. */
    public boolean isReachable(String node, String start, Set<String> owned) {
        for (String n : neighbours(node)) {
            if (n.equals(start) || owned.contains(n)) {
                return true;
            }
        }
        return false;
    }

    /** True if removing {@code node} leaves every other owned node connected to {@code start}. */
    public boolean canRemove(String node, String start, Set<String> owned) {
        Set<String> remaining = new HashSet<>(owned);
        remaining.remove(node);
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            for (String n : neighbours(queue.poll())) {
                if (remaining.contains(n) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        seen.remove(start);
        return seen.containsAll(remaining);
    }
}

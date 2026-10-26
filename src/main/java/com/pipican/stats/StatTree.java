package com.pipican.stats;

import java.util.*;

/**
 * General stat tree only. Class skill trees are separate.
 * Nodes cost stat points and can grant primary attribute levels and/or passive effects.
 * Unlock IDs must be persisted beside StatState; server is authoritative.
 */
public final class StatTree {
    public record Node(String id, int cost, Map<Stat,Integer> attributeBonuses,
                       List<EffectModifier> passives, Set<String> prerequisites) {
        public Node {
            if (id == null || id.isBlank() || cost <= 0) throw new IllegalArgumentException("node");
            attributeBonuses = Map.copyOf(attributeBonuses);
            passives = List.copyOf(passives);
            prerequisites = Set.copyOf(prerequisites);
            if (prerequisites.contains(id)) throw new IllegalArgumentException("self prerequisite");
            attributeBonuses.forEach((s,n) -> { if(n <= 0) throw new IllegalArgumentException("bonus"); });
        }
        /** Backwards-compatible primary-stat upgrade node. */
        public Node(String id, Stat stat, int points, Set<String> prerequisites) {
            this(id, points, Map.of(stat,points), List.of(), prerequisites);
        }
    }
    private final Map<String,Node> nodes;
    public StatTree(Collection<Node> nodes) {
        Map<String,Node> result = new HashMap<>();
        for (Node n : nodes) if (result.putIfAbsent(n.id(), n) != null) throw new IllegalArgumentException("duplicate node");
        for (Node n : result.values()) for (String parent : n.prerequisites())
            if (!result.containsKey(parent)) throw new IllegalArgumentException("missing prerequisite " + parent);
        for (String id : result.keySet()) checkCycle(id, result, new HashSet<>(), new HashSet<>());
        this.nodes = Map.copyOf(result);
    }
    private static void checkCycle(String id, Map<String,Node> nodes, Set<String> visiting, Set<String> done) {
        if (done.contains(id)) return;
        if (!visiting.add(id)) throw new IllegalArgumentException("cyclic tree");
        for (String p : nodes.get(id).prerequisites()) checkCycle(p, nodes, visiting, done);
        visiting.remove(id); done.add(id);
    }
    public Map<String,Node> nodes() { return nodes; }
    public boolean canUnlock(String id, Set<String> unlocked) {
        Node n = nodes.get(id);
        return n != null && !unlocked.contains(id) && unlocked.containsAll(n.prerequisites());
    }
    public List<EffectModifier> activePassives(Set<String> unlocked) {
        List<EffectModifier> result = new ArrayList<>();
        for (String id : unlocked) {
            Node node = nodes.get(id);
            if (node != null) result.addAll(node.passives());
        }
        return List.copyOf(result);
    }
    /** On success spends node cost once and stores unlocked ID. */
    public void unlock(String id, Set<String> unlocked, StatState state, StatEngine engine) {
        if (!canUnlock(id, unlocked)) throw new IllegalArgumentException("locked node");
        Node node = nodes.get(id);
        engine.spendTreeNode(state, node.cost(), node.attributeBonuses());
        unlocked.add(id);
    }
}

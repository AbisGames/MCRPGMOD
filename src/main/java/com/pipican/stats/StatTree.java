package com.pipican.stats;

import java.util.*;

/** Data-driven passive stat tree. Nodes provide primary-stat increments, not class skills. */
public final class StatTree {
    public record Node(String id, Stat stat, int points, Set<String> prerequisites) {
        public Node {
            if (id == null || id.isBlank() || stat == null || points <= 0) throw new IllegalArgumentException("node");
            prerequisites = Set.copyOf(prerequisites);
            if (prerequisites.contains(id)) throw new IllegalArgumentException("self prerequisite");
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
    /** Apply node cost as invested points; persistence stores unlocked node ids separately. */
    public void unlock(String id, Set<String> unlocked, StatState state, StatEngine engine) {
        if (!canUnlock(id, unlocked)) throw new IllegalArgumentException("locked node");
        Node node = nodes.get(id);
        engine.allocate(state, Map.of(node.stat(), node.points()));
        unlocked.add(id);
    }
}

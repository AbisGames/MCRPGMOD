package com.pipican.worldrpg;

import java.util.*;

/**
 * Immutable quest template. A regular quest-giver owns exactly one quest ID.
 * Trainer questlines instead contain an ordered list of these templates.
 */
public record QuestDefinition(String id, Set<String> allowedClasses, List<Objective> objectives,
                              Reward reward, Set<String> prerequisites) {
    public enum ObjectiveType { KILL_ENTITY, DELIVER_ITEM, VISIT_STRUCTURE, TALK_TO_NPC, TARGET_HITS, CLEAR_STRUCTURE }
    public record Objective(String id, ObjectiveType type, String targetId, int required) {
        public Objective {
            if (id == null || id.isBlank() || type == null || targetId == null || targetId.isBlank() || required <= 0)
                throw new IllegalArgumentException("objective");
        }
    }
    public record Reward(long coins, Map<String,Integer> items, Set<String> recipes) {
        public Reward {
            if (coins < 0) throw new IllegalArgumentException("coins");
            items = Map.copyOf(items); recipes = Set.copyOf(recipes);
            items.forEach((k,v) -> { if (k.isBlank() || v <= 0) throw new IllegalArgumentException("item reward"); });
        }
    }
    public QuestDefinition {
        if (id == null || id.isBlank() || objectives == null || objectives.isEmpty() || reward == null)
            throw new IllegalArgumentException("quest");
        allowedClasses = Set.copyOf(allowedClasses);
        objectives = List.copyOf(objectives);
        prerequisites = Set.copyOf(prerequisites);
        if (objectives.stream().map(Objective::id).distinct().count() != objectives.size())
            throw new IllegalArgumentException("duplicate objective");
        if (prerequisites.contains(id)) throw new IllegalArgumentException("self prerequisite");
    }
}

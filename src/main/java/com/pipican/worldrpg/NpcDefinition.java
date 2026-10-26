package com.pipican.worldrpg;

import java.util.*;

/**
 * NPC template is independent of world-generation implementation.
 * Exactly one ordinary quest ID, or one trainer questline, never both.
 */
public record NpcDefinition(String id, Role role, String profession, String structureTag,
                            String behaviorProfile, Optional<String> questId,
                            Optional<String> trainerId, Optional<String> shopId) {
    public enum Role { AMBIENT, QUEST_GIVER, TRAINER, DOJO_MASTER, MERCHANT, SERVICE }
    public NpcDefinition {
        if (id == null || id.isBlank() || role == null || profession == null || profession.isBlank()
            || structureTag == null || structureTag.isBlank() || behaviorProfile == null || behaviorProfile.isBlank())
            throw new IllegalArgumentException("npc");
        questId = Objects.requireNonNull(questId);
        trainerId = Objects.requireNonNull(trainerId);
        shopId = Objects.requireNonNull(shopId);
        if (questId.isPresent() && trainerId.isPresent()) throw new IllegalArgumentException("one quest OR trainer");
        if (role == Role.AMBIENT && (questId.isPresent() || trainerId.isPresent()))
            throw new IllegalArgumentException("ambient npc cannot assign quests");
        if (role == Role.QUEST_GIVER && questId.isEmpty())
            throw new IllegalArgumentException("quest NPC must provide one quest");
        if (role == Role.TRAINER && trainerId.isEmpty())
            throw new IllegalArgumentException("trainer must provide a questline");
    }
}

package com.pipican.worldrpg;

import java.util.*;

/** Class unlocks are awarded by completing the *entire* training questline. */
public record TrainerDefinition(String id, String unlockClassId, List<String> questline) {
    public TrainerDefinition {
        if (id == null || id.isBlank() || unlockClassId == null || unlockClassId.isBlank()
            || questline == null || questline.isEmpty()) throw new IllegalArgumentException("trainer");
        questline = List.copyOf(questline);
        if (questline.stream().distinct().count() != questline.size()) throw new IllegalArgumentException("duplicate quest");
    }
}

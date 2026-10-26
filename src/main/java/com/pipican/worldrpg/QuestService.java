package com.pipican.worldrpg;

import java.util.*;

/**
 * Progress only accepted while a quest is available to this player.
 * Never accept objective completion directly from an untrusted client packet.
 */
public final class QuestService {
    public record Event(QuestDefinition.ObjectiveType type, String targetId, int amount) {}
    private final WorldRpgCatalog catalog;
    public QuestService(WorldRpgCatalog catalog) { this.catalog=Objects.requireNonNull(catalog); }
    public boolean eligible(WorldRpgState state, QuestDefinition quest, Optional<String> classId) {
        if(state.completed().contains(quest.id()) || !state.completed().containsAll(quest.prerequisites())) return false;
        if (state.training().isPresent()) {
            TrainerDefinition trainer = catalog.trainer(state.training().orElseThrow().trainerId()).orElseThrow();
            return trainer.questline().get(state.training().orElseThrow().questIndex()).equals(quest.id());
        }
        return quest.allowedClasses().isEmpty() || classId.filter(quest.allowedClasses()::contains).isPresent();
    }
    public boolean progress(WorldRpgState state, String questId, Optional<String> currentClass, Event event) {
        QuestDefinition quest = catalog.quest(questId).orElseThrow();
        if (!eligible(state, quest, currentClass) || event == null || event.amount() <= 0) return false;
        boolean changed=false;
        for (var objective : quest.objectives()) {
            if(objective.type()==event.type() && objective.targetId().equals(event.targetId())
               && state.count(questId,objective.id())<objective.required()) {
                state.increment(questId,objective.id(),event.amount(),objective.required()); changed=true;
            }
        }
        return changed;
    }
    public boolean ready(WorldRpgState state, QuestDefinition quest) {
        return quest.objectives().stream().allMatch(o->state.count(quest.id(),o.id())>=o.required());
    }
    /** Rewards must be applied by a transactional server adapter; call after inventory capacity checks. */
    public QuestDefinition.Reward complete(WorldRpgState state, String questId, Optional<String> currentClass) {
        QuestDefinition q=catalog.quest(questId).orElseThrow();
        if (!eligible(state,q,currentClass) || !ready(state,q)) throw new IllegalStateException("not complete");
        // Inventory item grants are handled transactionally by the NeoForge adapter.
        // Check overflow before marking quest completion to prevent partial updates.
        Math.addExact(state.coins(), q.reward().coins());
        state.addCoins(q.reward().coins());
        q.reward().recipes().forEach(state::unlockRecipe);
        state.rewardOnce(questId);
        state.complete(questId);
        return q.reward();
    }
}

package com.pipican.worldrpg;

import java.util.*;

/**
 * Class integration boundary. Host must suppress all class attributes, effects,
 * skills, special resource and mastery *server-side* whenever training is active.
 */
public final class TrainingAndDojoService {
    public interface ClassHost {
        Optional<String> currentClass();
        void setEffectiveClass(Optional<String> classId);
        void setPowersAndMasterySuspended(boolean suspended);
        boolean inCombat();
    }
    private final WorldRpgCatalog catalog;
    public TrainingAndDojoService(WorldRpgCatalog catalog) {this.catalog=catalog;}
    public String start(WorldRpgState state, String trainerId, ClassHost host) {
        if(state.training().isPresent()) throw new IllegalStateException("already training");
        TrainerDefinition trainer=catalog.trainer(trainerId).orElseThrow();
        if(state.unlockedClasses().contains(trainer.unlockClassId())) throw new IllegalStateException("already unlocked");
        if(host.inCombat()) throw new IllegalStateException("cannot train during combat");
        String previous=host.currentClass().orElse(null);
        state.training(new WorldRpgState.Training(trainerId,previous,0));
        host.setPowersAndMasterySuspended(true);
        host.setEffectiveClass(Optional.empty());
        return trainer.questline().getFirst();
    }
    /** Called only when QuestService has fully completed the current training quest. */
    public Optional<String> advance(WorldRpgState state, String completedQuestId, ClassHost host) {
        var training=state.training().orElseThrow();
        var trainer=catalog.trainer(training.trainerId()).orElseThrow();
        if(!trainer.questline().get(training.questIndex()).equals(completedQuestId)
            || !state.completed().contains(completedQuestId)) throw new IllegalStateException("wrong training quest");
        int next=training.questIndex()+1;
        if(next<trainer.questline().size()) {
            state.training(new WorldRpgState.Training(training.trainerId(),training.previousClassId(),next));
            return Optional.of(trainer.questline().get(next));
        }
        state.unlockClass(trainer.unlockClassId());
        end(state,host);
        return Optional.empty();
    }
    public void cancel(WorldRpgState state, ClassHost host) {if(state.training().isPresent()) end(state,host);}
    private void end(WorldRpgState state, ClassHost host) {
        String old=state.training().orElseThrow().previousClassId();
        state.training(null);
        host.setPowersAndMasterySuspended(false);
        host.setEffectiveClass(Optional.ofNullable(old));
    }
    /** Only invoke from a verified dojo-master interaction. */
    public void assignDojoSlots(WorldRpgState state,String a,String b) {
        if(state.training().isPresent()) throw new IllegalStateException("training");
        if(a!=null && !state.unlockedClasses().contains(a) || b!=null && !state.unlockedClasses().contains(b)
           || a!=null && a.equals(b)) throw new IllegalArgumentException("invalid slots");
        state.slots(a,b);
    }
    /** Keybind calls host after a server-validated cooldown/combat check. */
    public void quickSwap(WorldRpgState state,ClassHost host) {
        if(state.training().isPresent() || host.inCombat()) throw new IllegalStateException("cannot swap");
        String next=Objects.equals(host.currentClass().orElse(null),state.slotA().orElse(null))
            ?state.slotB().orElse(null):state.slotA().orElse(null);
        if(next==null || !state.unlockedClasses().contains(next)) throw new IllegalStateException("empty slot");
        host.setEffectiveClass(Optional.of(next)); state.equip(next);
    }
}

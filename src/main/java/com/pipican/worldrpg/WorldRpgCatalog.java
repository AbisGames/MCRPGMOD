package com.pipican.worldrpg;

import java.util.*;

/** Immutable and validated catalogs; use a future datapack reload adapter to construct them. */
public final class WorldRpgCatalog {
    private final Map<String,QuestDefinition> quests;
    private final Map<String,NpcDefinition> npcs;
    private final Map<String,TrainerDefinition> trainers;
    public WorldRpgCatalog(Collection<QuestDefinition> quests, Collection<NpcDefinition> npcs,
                           Collection<TrainerDefinition> trainers) {
        this.quests = unique(quests, QuestDefinition::id);
        this.npcs = unique(npcs, NpcDefinition::id);
        this.trainers = unique(trainers, TrainerDefinition::id);
        for (QuestDefinition q : this.quests.values())
            for (String predecessor : q.prerequisites())
                if (!this.quests.containsKey(predecessor)) throw new IllegalArgumentException("missing quest prerequisite: "+predecessor);
        for (NpcDefinition npc : this.npcs.values()) {
            npc.questId().ifPresent(id -> { if (!this.quests.containsKey(id)) throw new IllegalArgumentException("unknown quest "+id); });
            npc.trainerId().ifPresent(id -> { if (!this.trainers.containsKey(id)) throw new IllegalArgumentException("unknown trainer "+id); });
        }
        for (TrainerDefinition t : this.trainers.values())
            for (String id : t.questline())
                if (!this.quests.containsKey(id)) throw new IllegalArgumentException("missing training quest "+id);
    }
    private static <T> Map<String,T> unique(Collection<T> input, java.util.function.Function<T,String> id) {
        Map<String,T> m = new HashMap<>();
        for (T item : input) if (m.putIfAbsent(id.apply(item),item) != null) throw new IllegalArgumentException("duplicate id: "+id.apply(item));
        return Map.copyOf(m);
    }
    public Optional<QuestDefinition> quest(String id) { return Optional.ofNullable(quests.get(id)); }
    public Optional<NpcDefinition> npc(String id) { return Optional.ofNullable(npcs.get(id)); }
    public Optional<TrainerDefinition> trainer(String id) { return Optional.ofNullable(trainers.get(id)); }
    public Collection<QuestDefinition> quests() { return quests.values(); }
    public Collection<NpcDefinition> npcs() { return npcs.values(); }
    public Collection<TrainerDefinition> trainers() { return trainers.values(); }
}

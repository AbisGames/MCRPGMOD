package com.pipican.worldrpg;

import java.util.*;

/**
 * Mutable per-player server-authoritative state. Persistence adapter must save every field.
 * Active training is exclusive and temporarily suppresses ALL class powers and mastery.
 */
public final class WorldRpgState {
    public record Training(String trainerId, String previousClassId, int questIndex) {}
    private final Set<String> completed = new HashSet<>();
    /** A quest cannot be progressed until the player explicitly accepts it. */
    private final Set<String> accepted = new HashSet<>();
    private final Map<String,Map<String,Integer>> objectiveCounts = new HashMap<>();
    private final Set<String> rewarded = new HashSet<>();
    private final Set<String> unlockedClasses = new HashSet<>();
    private final Set<String> unlockedRecipes = new HashSet<>();
    private final Set<String> visitedStructures = new HashSet<>();
    private long coins;
    private String slotA, slotB, equippedClass;
    private Training training;
    public long coins() { return coins; }
    public Set<String> completed() { return Set.copyOf(completed); }
    public Set<String> accepted() { return Set.copyOf(accepted); }
    void acceptQuest(String id) { accepted.add(id); }
    void finishQuest(String id) { accepted.remove(id); complete(id); }
    public Set<String> rewarded() { return Set.copyOf(rewarded); }
    public Set<String> unlockedClasses() { return Set.copyOf(unlockedClasses); }
    public Set<String> unlockedRecipes() { return Set.copyOf(unlockedRecipes); }
    public Set<String> visitedStructures() { return Set.copyOf(visitedStructures); }
    public Map<String,Map<String,Integer>> objectiveCounts() {
        Map<String,Map<String,Integer>> result = new HashMap<>();
        objectiveCounts.forEach((k,v) -> result.put(k, Map.copyOf(v)));
        return Map.copyOf(result);
    }
    public Optional<Training> training() { return Optional.ofNullable(training); }
    public Optional<String> equippedClass() { return Optional.ofNullable(equippedClass); }
    public Optional<String> slotA() { return Optional.ofNullable(slotA); }
    public Optional<String> slotB() { return Optional.ofNullable(slotB); }
    public boolean classPowersSuspended() { return training != null; }
    void training(Training value) { training = value; }
    void equip(String id) { equippedClass = id; }
    void slots(String a, String b) { slotA = a; slotB = b; }
    void unlockClass(String id) { unlockedClasses.add(id); }
    void unlockRecipe(String id) { unlockedRecipes.add(id); }
    void visit(String id) { visitedStructures.add(id); }
    void complete(String id) { completed.add(id); }
    boolean rewardOnce(String id) { return rewarded.add(id); }
    void addCoins(long amount) { if(amount < 0) throw new IllegalArgumentException("negative"); coins = Math.addExact(coins,amount); }
    void pay(long amount) {
        if (amount < 0 || amount > coins) throw new IllegalArgumentException("insufficient coins");
        coins -= amount;
    }
    int increment(String quest, String objective, int change, int limit) {
        if (change <= 0) throw new IllegalArgumentException("change");
        Map<String,Integer> map = objectiveCounts.computeIfAbsent(quest,k->new HashMap<>());
        int old = map.getOrDefault(objective,0);
        int value = (int)Math.min(limit, (long)old + change);
        map.put(objective,value); return value;
    }
    int count(String quest, String objective) { return objectiveCounts.getOrDefault(quest,Map.of()).getOrDefault(objective,0); }
    /** Restoring saved state must be validated and performed by a trusted server adapter. */
    public void restore(long coins, Collection<String> completed, Collection<String> rewarded,
                        Map<String,Map<String,Integer>> counts, Collection<String> classes,
                        Collection<String> recipes, Collection<String> visited,
                        String a, String b, String equipped, Training training) {
        if(coins<0 || counts.values().stream().flatMap(m->m.values().stream()).anyMatch(n->n<0))
            throw new IllegalArgumentException("invalid save");
        this.coins=coins; this.completed.clear(); this.completed.addAll(completed);
        // Legacy saves did not track acceptance. Re-accept incomplete quests explicitly.
        this.accepted.clear();
        this.rewarded.clear(); this.rewarded.addAll(rewarded);
        this.objectiveCounts.clear(); counts.forEach((k,v)->this.objectiveCounts.put(k,new HashMap<>(v)));
        this.unlockedClasses.clear(); this.unlockedClasses.addAll(classes);
        this.unlockedRecipes.clear(); this.unlockedRecipes.addAll(recipes);
        this.visitedStructures.clear(); this.visitedStructures.addAll(visited);
        this.slotA=a; this.slotB=b; this.equippedClass=equipped; this.training=training;
    }
}

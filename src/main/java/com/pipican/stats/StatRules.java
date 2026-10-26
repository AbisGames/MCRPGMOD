package com.pipican.stats;

import java.util.*;

/**
 * Data-driven attribute -> effect conversions. Rates are per one bonus stat point,
 * multiplied by the owning class's scaling for that attribute.
 * No registry changes needed: the eight core stats remain fixed.
 */
public final class StatRules {
    private final Map<Stat,List<EffectModifier>> conversions;
    private final Map<Effect,Double> baseEffects;
    private final double maxMovementMultiplier;
    private final int agilityPointsPerIFrame;

    public StatRules(Map<Stat,List<EffectModifier>> conversions,
                     Map<Effect,Double> baseEffects,
                     double maxMovementMultiplier, int agilityPointsPerIFrame) {
        if (!Double.isFinite(maxMovementMultiplier) || maxMovementMultiplier < 1 || maxMovementMultiplier > 2)
            throw new IllegalArgumentException("movement cap must be in [1,2]");
        if (agilityPointsPerIFrame <= 0) throw new IllegalArgumentException("iframe step");
        EnumMap<Stat,List<EffectModifier>> copy = new EnumMap<>(Stat.class);
        conversions.forEach((k,v) -> copy.put(Objects.requireNonNull(k), List.copyOf(v)));
        EnumMap<Effect,Double> bases = new EnumMap<>(Effect.class);
        baseEffects.forEach((k,v) -> {
            if (!Double.isFinite(v)) throw new IllegalArgumentException("base effect");
            bases.put(k,v);
        });
        this.conversions = Collections.unmodifiableMap(copy);
        this.baseEffects = Collections.unmodifiableMap(bases);
        this.maxMovementMultiplier = maxMovementMultiplier;
        this.agilityPointsPerIFrame = agilityPointsPerIFrame;
    }
    public List<EffectModifier> conversions(Stat stat) { return conversions.getOrDefault(stat,List.of()); }
    public double base(Effect effect) { return baseEffects.getOrDefault(effect,0.0); }
    public double maxMovementMultiplier() { return maxMovementMultiplier; }
    public int agilityPointsPerIFrame() { return agilityPointsPerIFrame; }
    public static StatRules defaults() {
        return new StatRules(Map.of(
            Stat.VITALITY, List.of(new EffectModifier(Effect.MAX_HP, EffectModifier.Operation.FLAT, 1)),
            Stat.STRENGTH, List.of(new EffectModifier(Effect.PHYSICAL_DAMAGE, EffectModifier.Operation.FLAT, 0.1)),
            Stat.INTELLIGENCE, List.of(new EffectModifier(Effect.MAGIC_DAMAGE, EffectModifier.Operation.FLAT, 0.1)),
            Stat.AGILITY, List.of(new EffectModifier(Effect.MOVEMENT_SPEED, EffectModifier.Operation.FLAT, 0.005)),
            Stat.DEXTERITY, List.of(new EffectModifier(Effect.ATTACK_SPEED, EffectModifier.Operation.FLAT, 0.002)),
            Stat.WISDOM, List.of(new EffectModifier(Effect.HEALING_POWER, EffectModifier.Operation.FLAT, 0.01)),
            Stat.ENDURANCE, List.of(new EffectModifier(Effect.HEALTH_REGEN, EffectModifier.Operation.FLAT, 0.005)),
            Stat.LUCK, List.of(new EffectModifier(Effect.CRITICAL_CHANCE, EffectModifier.Operation.FLAT, 0.001))
        ), Map.of(Effect.MOVEMENT_SPEED, 1.0), 2.0, 20);
    }
}

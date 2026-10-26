package com.pipican.stats;

import java.util.*;

/**
 * Computes effects without changing NeoForge attributes directly. Loader adapters must
 * apply results with stable modifier IDs, never stacking the same calculation each tick.
 */
public final class StatCalculator {
    public record Equipment(Map<Stat,Integer> bonuses, double armor, double magicResistance) {
        public Equipment {
            bonuses = Map.copyOf(bonuses);
            if (!Double.isFinite(armor) || armor < 0 || !Double.isFinite(magicResistance) || magicResistance < 0)
                throw new IllegalArgumentException("equipment defenses");
        }
        public static Equipment empty() { return new Equipment(Map.of(), 0, 0); }
    }
    public record Derived(double maxHp, double movementMultiplier, int dodgeBonusTicks,
                          double physicalDamageBonus, double magicDamageBonus,
                          double armor, double magicResistance) {}
    private final StatRules rules;
    public StatCalculator() { this(StatRules.defaults()); }
    public StatCalculator(StatRules rules) { this.rules = Objects.requireNonNull(rules); }

    public int effective(StatState state, StatProfile profile, Equipment equipment, Stat stat) {
        return Math.addExact(Math.addExact(profile.starting(stat), state.invested(stat)),
                             equipment.bonuses().getOrDefault(stat, 0));
    }
    public Map<Stat,Integer> effectiveAll(StatState state, StatProfile profile, Equipment equipment) {
        EnumMap<Stat,Integer> values = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) values.put(stat, effective(state, profile, equipment, stat));
        return Map.copyOf(values);
    }
    /**
     * Base stat means class starting points: already represented by class base HP etc.
     * Additional invested/equipment points use per-class conversions.
     * Passives from stat tree/class skills/equipment can be combined in the modifier list.
     */
    public Map<Effect,Double> effects(StatState state, StatProfile profile, Equipment equipment,
                                      Collection<EffectModifier> passives) {
        EnumMap<Effect,Double> flat = new EnumMap<>(Effect.class);
        EnumMap<Effect,Double> percent = new EnumMap<>(Effect.class);
        for (Effect e : Effect.values()) flat.put(e, rules.base(e));
        flat.put(Effect.MAX_HP, profile.baseHp());
        for (Stat stat : Stat.values()) {
            int bonus = Math.addExact(state.invested(stat), equipment.bonuses().getOrDefault(stat, 0));
            for (EffectModifier conversion : rules.conversions(stat)) {
                double value = bonus * profile.scaling(stat) * conversion.value();
                accumulate(flat, percent, conversion.effect(), conversion.operation(), value);
            }
        }
        // Dodge bonus derives from actual AGI bonus only. Scaling is class-dependent;
        // floor after scaling prevents fractional ticks from granting invulnerability.
        int agility = Math.max(0, Math.addExact(state.invested(Stat.AGILITY),
            equipment.bonuses().getOrDefault(Stat.AGILITY, 0)));
        flat.put(Effect.DODGE_IFRAME_TICKS, Math.floor(agility * profile.scaling(Stat.AGILITY) / rules.agilityPointsPerIFrame()));
        for (EffectModifier modifier : passives) {
            accumulate(flat, percent, modifier.effect(), modifier.operation(), modifier.value());
        }
        EnumMap<Effect,Double> result = new EnumMap<>(Effect.class);
        for (Effect e : Effect.values()) {
            double value = flat.getOrDefault(e, 0.0) * (1 + percent.getOrDefault(e, 0.0) / 100.0);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("effect overflow: " + e);
            if (e == Effect.MOVEMENT_SPEED) value = Math.max(0, Math.min(rules.maxMovementMultiplier(), value));
            else value = Math.max(0, value);
            if (e == Effect.DODGE_IFRAME_TICKS) value = Math.floor(value);
            result.put(e, value);
        }
        return Map.copyOf(result);
    }
    private static void accumulate(Map<Effect,Double> flat, Map<Effect,Double> percent,
                                   Effect effect, EffectModifier.Operation op, double value) {
        (op == EffectModifier.Operation.FLAT ? flat : percent).merge(effect, value, Double::sum);
    }
    public Derived derive(StatState state, StatProfile profile, Equipment equipment) {
        return derive(state, profile, equipment, List.of());
    }
    public Derived derive(StatState state, StatProfile profile, Equipment equipment,
                          Collection<EffectModifier> passives) {
        Map<Effect,Double> e = effects(state,profile,equipment,passives);
        return new Derived(e.get(Effect.MAX_HP), e.get(Effect.MOVEMENT_SPEED),
            (int)Math.min(Integer.MAX_VALUE,e.get(Effect.DODGE_IFRAME_TICKS)),
            e.get(Effect.PHYSICAL_DAMAGE), e.get(Effect.MAGIC_DAMAGE),
            equipment.armor(), equipment.magicResistance());
    }
    /** Missing requirements are reported for equipment penalty processing, not blocked. */
    public boolean meets(Map<Stat,Integer> requirements, StatState state, StatProfile profile, Equipment equipment) {
        for (var e : requirements.entrySet())
            if (effective(state,profile,equipment,e.getKey()) < e.getValue()) return false;
        return true;
    }
}

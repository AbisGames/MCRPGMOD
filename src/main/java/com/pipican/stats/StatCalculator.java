package com.pipican.stats;

import java.util.EnumMap;
import java.util.Map;

/**
 * Equipment gives flat attribute bonuses and armor/magic resistance separately.
 * Class multipliers affect gains from invested stats only, not equipment requirements.
 * Derived values are intentionally modest defaults pending balance approval.
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
    public int effective(StatState state, StatProfile profile, Equipment equipment, Stat stat) {
        return Math.addExact(Math.addExact(profile.starting(stat), state.invested(stat)),
                             equipment.bonuses().getOrDefault(stat, 0));
    }
    public Map<Stat,Integer> effectiveAll(StatState state, StatProfile profile, Equipment equipment) {
        EnumMap<Stat,Integer> values = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) values.put(stat, effective(state, profile, equipment, stat));
        return Map.copyOf(values);
    }
    public Derived derive(StatState state, StatProfile profile, Equipment equipment) {
        int vitBonus = state.invested(Stat.VITALITY) + equipment.bonuses().getOrDefault(Stat.VITALITY, 0);
        int agiBonus = state.invested(Stat.AGILITY) + equipment.bonuses().getOrDefault(Stat.AGILITY, 0);
        double hp = profile.baseHp() + vitBonus * profile.scaling(Stat.VITALITY);
        // Hard 2x movement cap, very small i-frame gain: +1 tick each 20 effective bonus AGI.
        double speed = Math.min(2.0, 1.0 + Math.max(0, agiBonus) * profile.scaling(Stat.AGILITY) * 0.005);
        int iframeTicks = Math.max(0, agiBonus / 20);
        double physical = Math.max(0, state.invested(Stat.STRENGTH)
            + equipment.bonuses().getOrDefault(Stat.STRENGTH, 0)) * profile.scaling(Stat.STRENGTH) * 0.1;
        double magic = Math.max(0, state.invested(Stat.INTELLIGENCE)
            + equipment.bonuses().getOrDefault(Stat.INTELLIGENCE, 0)) * profile.scaling(Stat.INTELLIGENCE) * 0.1;
        return new Derived(hp, speed, iframeTicks, physical, magic, equipment.armor(), equipment.magicResistance());
    }
    /** Requirement failure is reported to equipment system for heavy penalties, never hard blocked. */
    public boolean meets(Map<Stat,Integer> requirements, StatState state, StatProfile profile, Equipment equipment) {
        for (var e : requirements.entrySet()) if (effective(state, profile, equipment, e.getKey()) < e.getValue()) return false;
        return true;
    }
}

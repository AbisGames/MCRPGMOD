package com.pipican.player;

import com.pipican.effect.ClassEffect;
import com.pipican.condition.ClassCondition;
import com.pipican.effect.VictimState;
import com.pipican.effect.type.BonusEffect;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The computed (never saved) result of a player's class: total stats, damage multipliers, granted
 * potion effects and resource numbers. Rebuilt by {@link ClassEffectApplier#refresh} whenever
 * anything that affects it changes, then read by event handlers every hit / tick.
 */
public record ClassState(
        Optional<RpgClass> rpgClass,
        Map<ResourceKey<StatDefinition>, Double> stats,
        List<DamageRule> damageTaken,
        List<DamageRule> damageDealt,
        Map<Holder<MobEffect>, Integer> mobEffects,
        Set<Holder<Attribute>> touchedAttributes,
        double resourceMax,
        double resourceRegenPerTick,
        List<ConditionCheck> conditions,
        boolean flight,
        List<ReactionEffect> reactions,
        Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> eidolonUpgrades,
        int spellPoints,
        double optimizationAmount,
        List<Critical> criticals,
        double dodgeChance,
        double meleeDodgeChance,
        Set<Holder<MobEffect>> immunities,
        Map<BonusEffect.Kind, Double> bonuses,
        Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> mountUpgrades,
        Bleed bleed,
        Map<String, ReactionChance> reactionChances,
        double rangedDodgeChance,
        List<Execute> executes,
        List<Aura> auras,
        Scrutiny scrutiny,
        boolean favoredWeapon,
        SummonMods summonMods,
        Cleave cleave,
        PetMods petMods,
        Map<Holder<MobEffect>, AppliedRule> appliedRules,
        Map<Holder<MobEffect>, Double> effectDurations,
        List<com.pipican.effect.type.IgnoredByEffect> ignoredBy,
        Map<ResourceKey<com.pipican.ability.Ability>, AbilityMod> abilityMods,
        Map<String, ReactionMod> reactionMods,
        Shield shield,
        DeadlyAim deadlyAim,
        List<CraftBonus> craftBonuses,
        Map<String, Quarry> quarries,
        List<NthHit> nthHits) {

    public static final ClassState EMPTY = new ClassState(Optional.empty(), Map.of(), List.of(), List.of(), Map.of(), Set.of(), 0, 0, List.of(), false,
            List.of(), Map.of(), 0, 0, List.of(), 0, 0, Set.of(), Map.of(), Map.of(), Bleed.NONE, Map.of(), 0, List.of(), List.of(), Scrutiny.NONE, false, SummonMods.NONE, Cleave.NONE, PetMods.NONE, Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Shield.NONE, DeadlyAim.NONE, List.of(), Map.of(), List.of());

    /** A condition the state depends on, and what it answered when the state was computed. */
    public record ConditionCheck(ClassCondition condition, boolean result) {}

    /**
     * A damage multiplier, optionally limited to damage types in a tag, the weapon used, the kind of mob
     * on the other side ({@code against}) and the victim's state.
     */
    public record DamageRule(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, Optional<HolderSet<EntityType<?>>> against,
                             Optional<VictimState> victim, double multiplier, boolean except) {

        public DamageRule(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier) {
            this(types, weapons, Optional.empty(), Optional.empty(), multiplier, false);
        }

        /**
         * @param other the other side: the mob hit (damage dealt) or the attacker (damage taken); may be null
         * @param self  the player the rule belongs to (needed for victim-state checks); may be null
         */
        public boolean matches(DamageSource source, @Nullable LivingEntity other, @Nullable LivingEntity self) {
            if (types.isPresent() && !source.is(types.get())) {
                return false;
            }
            if (weapons.isPresent()) {
                ItemStack weapon = source.getWeaponItem();
                if (weapon == null || weapon.isEmpty() || !weapon.is(weapons.get())) {
                    return false;
                }
            }
            boolean hit = (against.isEmpty() || other != null && against.get().contains(other.getType().builtInRegistryHolder()))
                    && (victim.isEmpty() || other != null && self != null && victim.get().test(source, self, other));
            return except ? !hit : hit;
        }
    }

    /** Bleeding the player causes ({@code pipican:bleed} effects added up). */
    public record Bleed(double damage, double seconds, int maxStacks, boolean anyHit, boolean slows, Optional<HolderSet<Item>> weapons) {
        public static final Bleed NONE = new Bleed(0, 0, 1, false, false, Optional.empty());
    }

    /** A {@code pipican:execute}: below this fraction of max health, the player's hits kill. */
    public record Execute(double threshold, Optional<HolderSet<EntityType<?>>> victims) {}

    /** A {@code pipican:aura} the player gives nearby players. */
    public record Aura(double radius, List<ClassEffect> effects, double strength) {}

    /** {@code pipican:cleave} totals. */
    public record Cleave(double fraction, double radius, int maxTargets, boolean chargedOnly, Optional<HolderSet<Item>> weapons) {
        public static final Cleave NONE = new Cleave(0, 0, 0, false, Optional.empty());
    }

    /** A {@code pipican:quarry} with its {@code pipican:quarry_mod} upgrades folded in. */
    public record Quarry(com.pipican.effect.type.QuarryEffect.Pick pick, double seconds, boolean replace, boolean glow, double multiplier,
                         double othersDamage, Map<Holder<MobEffect>, Integer> effects, boolean forever, boolean melee, boolean endOnOtherHit,
                         double untouched) {
        public Quarry with(QuarryMod m) {
            Map<Holder<MobEffect>, Integer> all = new java.util.HashMap<>(effects);
            m.effects().forEach((k, v) -> all.merge(k, v, Math::max));
            return new Quarry(pick, seconds * Math.max(0, 1 + m.duration()), replace, glow, multiplier + m.multiplier(), othersDamage, Map.copyOf(all),
                    forever || m.forever(), melee || m.melee(), endOnOtherHit || m.endOnOtherHit(), untouched + m.untouched());
        }
    }

    /** A {@code pipican:quarry_mod}. */
    public record QuarryMod(double duration, double multiplier, double untouched, boolean forever, boolean melee, boolean endOnOtherHit,
                            Map<Holder<MobEffect>, Integer> effects) {
        public QuarryMod plus(QuarryMod o) {
            Map<Holder<MobEffect>, Integer> all = new java.util.HashMap<>(effects);
            o.effects.forEach((k, v) -> all.merge(k, v, Math::max));
            return new QuarryMod(duration + o.duration, multiplier + o.multiplier, untouched + o.untouched, forever || o.forever,
                    melee || o.melee, endOnOtherHit || o.endOnOtherHit, Map.copyOf(all));
        }
    }

    /** A {@code pipican:nth_hit}. */
    public record NthHit(int every, boolean sameTarget, double multiplier, boolean strikeTwice, Optional<HolderSet<net.minecraft.world.item.Item>> weapons) {}

    /** {@code pipican:summon_mod} totals. */
    public record SummonMods(double damage, double health, double seconds, int count, double hearts, double cap, double capMultiplier) {
        public static final SummonMods NONE = new SummonMods(0, 0, 0, 0, 0, 0, 0);
    }

    /** A {@code pipican:craft_bonus}. */
    public record CraftBonus(HolderSet<Item> items, double chance) {}

    /** {@code pipican:resource_shield} totals: damage absorbed per resource point (0 = off). */
    public record Shield(double perPoint) {
        public static final Shield NONE = new Shield(0);
    }

    /** {@code pipican:deadly_aim} totals (off unless a base effect counts). */
    public record DeadlyAim(boolean on, double holdSeconds, double damage, boolean crit) {
        public static final DeadlyAim NONE = new DeadlyAim(false, 0, 0, false);
    }

    /** {@code pipican:reaction_mod} totals for one reaction name. */
    public record ReactionMod(double seconds, double radius, double heal, double damage, List<com.pipican.ability.AbilityAction> actions, double strength,
                              Map<Holder<MobEffect>, Integer> amplify, double cooldown, int hits, double duration) {
        public static final ReactionMod NONE = new ReactionMod(0, 0, 0, 0, List.of(), 0, Map.of(), 0, 0, 0);

        public ReactionMod(double seconds, double radius, double heal, double damage, List<com.pipican.ability.AbilityAction> actions, double strength) {
            this(seconds, radius, heal, damage, actions, strength, Map.of(), 0, 0, 0);
        }

        public ReactionMod(double seconds, double radius, double heal, double damage, List<com.pipican.ability.AbilityAction> actions, double strength,
                           Map<Holder<MobEffect>, Integer> amplify) {
            this(seconds, radius, heal, damage, actions, strength, amplify, 0, 0, 0);
        }

        private static Map<Holder<MobEffect>, Integer> maxOf(Map<Holder<MobEffect>, Integer> a, Map<Holder<MobEffect>, Integer> b) {
            Map<Holder<MobEffect>, Integer> out = new java.util.HashMap<>(a);
            b.forEach((k, v) -> out.merge(k, v, Math::max));
            return Map.copyOf(out);
        }

        public ReactionMod plus(ReactionMod o) {
            List<com.pipican.ability.AbilityAction> all = new java.util.ArrayList<>(actions);
            all.addAll(o.actions);
            return new ReactionMod(seconds + o.seconds, radius + o.radius, heal + o.heal, damage + o.damage, List.copyOf(all), strength + o.strength,
                    maxOf(amplify, o.amplify), cooldown + o.cooldown, hits + o.hits, duration + o.duration);
        }
    }

    /** {@code pipican:ability_mod} totals for one ability. */
    public record AbilityMod(double cooldownSeconds, double cost, double drain, double duration, double radius, int uses, double strength,
                             double seconds, List<com.pipican.ability.AbilityAction> actions, Map<Holder<MobEffect>, Integer> amplify, boolean locked) {
        public static final AbilityMod NONE = new AbilityMod(0, 0, 0, 0, 0, 0, 0, 0, List.of(), Map.of(), false);

        public AbilityMod plus(AbilityMod o) {
            List<com.pipican.ability.AbilityAction> all = new java.util.ArrayList<>(actions);
            all.addAll(o.actions);
            return new AbilityMod(cooldownSeconds + o.cooldownSeconds, cost + o.cost, drain + o.drain, duration + o.duration, radius + o.radius,
                    uses + o.uses, strength + o.strength, seconds + o.seconds, List.copyOf(all), ReactionMod.maxOf(amplify, o.amplify),
                    locked || o.locked);
        }
    }

    public AbilityMod abilityMod(ResourceKey<com.pipican.ability.Ability> ability) {
        return ability == null ? AbilityMod.NONE : abilityMods.getOrDefault(ability, AbilityMod.NONE);
    }

    /** {@code pipican:applied_effect} totals for one effect. */
    public record AppliedRule(double seconds, double duration, int minAmplifier, Map<Holder<MobEffect>, Integer> also) {
        public AppliedRule plus(AppliedRule o) {
            Map<Holder<MobEffect>, Integer> merged = new java.util.LinkedHashMap<>(also);
            o.also.forEach((k, v) -> merged.merge(k, v, Math::max));
            return new AppliedRule(seconds + o.seconds, duration + o.duration, Math.max(minAmplifier, o.minAmplifier), Map.copyOf(merged));
        }
    }

    /** {@code pipican:pet_mod} totals. */
    public record PetMods(double damage, double health, double hearts, double armor, double damageTaken) {
        public static final PetMods NONE = new PetMods(0, 0, 0, 0, 0);

        public PetMods plus(PetMods o) {
            return new PetMods(damage + o.damage, health + o.health, hearts + o.hearts, armor + o.armor, damageTaken + o.damageTaken);
        }
    }

    /** {@code pipican:scrutiny} settings (seconds 0 = none). */
    public record Scrutiny(double lookSeconds, double seconds) {
        public static final Scrutiny NONE = new Scrutiny(0, 0);
    }

    /** Changes to named reactions' chances: (chance + add) x multiplier. */
    public record ReactionChance(double add, double multiplier, double uses) {}

    /** Extra uses per rest for a named reaction ({@code pipican:reaction_chance} uses). */
    public int reactionUses(ReactionEffect reaction) {
        ReactionChance change = reaction.name().map(reactionChances::get).orElse(null);
        return change == null ? 0 : (int) Math.floor(change.uses() + 1e-9);
    }

    /** A reaction's chance after {@code pipican:reaction_chance} effects. */
    public double reactionChance(ReactionEffect reaction) {
        if (reaction.name().isEmpty()) {
            return reaction.chance();
        }
        ReactionChance change = reactionChances.get(reaction.name().get());
        return change == null ? reaction.chance() : Math.min(1, Math.max(0, (reaction.chance() + change.add()) * change.multiplier()));
    }

    /** The summed chance of a chance-type {@code pipican:bonus} kind (extra loot...), not a multiplier. */
    public double chance(BonusEffect.Kind kind) {
        return bonuses.getOrDefault(kind, 0.0);
    }

    /** Critical-hit bonuses: crit damage (0.1 = +10%) and a chance to crit, for all weapons or only some. */
    public record Critical(double damage, double chance, Optional<HolderSet<Item>> weapons, Optional<VictimState> victim) {}

    /** The multiplier for a bonus kind: 1 + the summed amounts, never below 0. */
    public double bonus(BonusEffect.Kind kind) {
        return Math.max(0, 1 + bonuses.getOrDefault(kind, 0.0));
    }

    public double stat(ResourceKey<StatDefinition> stat) {
        return stats.getOrDefault(stat, 0.0);
    }

    public static double multiplier(List<DamageRule> rules, DamageSource source) {
        return multiplier(rules, source, null, null);
    }

    public static double multiplier(List<DamageRule> rules, DamageSource source, @Nullable LivingEntity other, @Nullable LivingEntity self) {
        double mult = 1;
        for (DamageRule rule : rules) {
            if (rule.matches(source, other, self)) {
                mult *= rule.multiplier();
            }
        }
        return mult;
    }
}

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
        boolean favoredWeapon) {

    public static final ClassState EMPTY = new ClassState(Optional.empty(), Map.of(), List.of(), List.of(), Map.of(), Set.of(), 0, 0, List.of(), false,
            List.of(), Map.of(), 0, 0, List.of(), 0, 0, Set.of(), Map.of(), Map.of(), Bleed.NONE, Map.of(), 0, List.of(), List.of(), Scrutiny.NONE, false);

    /** A condition the state depends on, and what it answered when the state was computed. */
    public record ConditionCheck(ClassCondition condition, boolean result) {}

    /**
     * A damage multiplier, optionally limited to damage types in a tag, the weapon used, the kind of mob
     * on the other side ({@code against}) and the victim's state.
     */
    public record DamageRule(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, Optional<HolderSet<EntityType<?>>> against,
                             Optional<VictimState> victim, double multiplier) {

        public DamageRule(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier) {
            this(types, weapons, Optional.empty(), Optional.empty(), multiplier);
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
            if (against.isPresent() && (other == null || !against.get().contains(other.getType().builtInRegistryHolder()))) {
                return false;
            }
            return victim.isEmpty() || other != null && self != null && victim.get().test(self, other);
        }
    }

    /** Bleeding the player causes ({@code pipican:bleed} effects added up). */
    public record Bleed(double damage, double seconds, int maxStacks, boolean anyHit, boolean slows, Optional<HolderSet<Item>> weapons) {
        public static final Bleed NONE = new Bleed(0, 0, 1, false, false, Optional.empty());
    }

    /** A {@code pipican:execute}: below this fraction of max health, the player's hits kill. */
    public record Execute(double threshold, Optional<HolderSet<EntityType<?>>> victims) {}

    /** A {@code pipican:aura} the player gives nearby players. */
    public record Aura(double radius, List<ClassEffect> effects) {}

    /** {@code pipican:scrutiny} settings (seconds 0 = none). */
    public record Scrutiny(double lookSeconds, double seconds) {
        public static final Scrutiny NONE = new Scrutiny(0, 0);
    }

    /** Changes to named reactions' chances: (chance + add) x multiplier. */
    public record ReactionChance(double add, double multiplier) {}

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
    public record Critical(double damage, double chance, Optional<HolderSet<Item>> weapons) {}

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

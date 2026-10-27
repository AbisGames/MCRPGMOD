package com.pipican.player;

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
        Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> mountUpgrades) {

    public static final ClassState EMPTY = new ClassState(Optional.empty(), Map.of(), List.of(), List.of(), Map.of(), Set.of(), 0, 0, List.of(), false,
            List.of(), Map.of(), 0, 0, List.of(), 0, 0, Set.of(), Map.of(), Map.of());

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

package com.pipican.effect;

import org.jspecify.annotations.Nullable;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.EntityType;
import java.util.List;
import com.pipican.condition.ClassCondition;
import com.pipican.effect.type.BonusEffect;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.player.ClassState;
import com.pipican.effect.type.ResourceEffect;
import com.pipican.stat.StatDefinition;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

/**
 * Collects the results of {@link ClassEffect}s while a player's bonuses are being recalculated.
 * Implemented by {@link com.pipican.player.ClassEffectApplier}; effects only ever talk to this
 * interface, so new effect types never need to touch the applier.
 */
public interface EffectContext {

    /** Value of a {@link Scaling} source ({@code class_level}, {@code special_stat}, or a stat id). */
    double sourceValue(String source);

    /**
     * Multiplier the effect is applied at. Always 1 today; kept so effects stay ready for stacking
     * sources (e.g. jewels or repeated bonuses) without changing every effect type.
     */
    int rank();

    void addStat(ResourceKey<StatDefinition> stat, double amount);

    void addAttribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation);

    void addMobEffect(Holder<MobEffect> effect, int amplifier);

    /** Multiplies incoming damage that matches the rule (damage type, weapon, attacker type). */
    void addDamageTaken(ClassState.DamageRule rule);

    /** Multiplies outgoing damage that matches the rule (damage type, weapon, victim type and state). */
    void addDamageDealt(ClassState.DamageRule rule);

    /** Critical hits: extra crit damage (0.1 = +10%) and a chance to make any hit a crit, optionally only with {@code weapons}. */
    void addCritical(double damage, double chance, Optional<HolderSet<Item>> weapons);

    /** A chance to completely avoid incoming hits (only melee hits if {@code meleeOnly}). */
    void addDodge(double chance, boolean meleeOnly);

    /** A {@code pipican:bonus} percentage (0.1 = +10%); amounts of one kind add up. */
    void addBonus(BonusEffect.Kind kind, double amount);

    /** Potion effects that can't be applied to the player. */
    void addImmunity(HolderSet<MobEffect> effects);

    /** Changes the resource bar's max or regen; {@code operation} is add_value or add_multiplied_base. */
    void addResource(ResourceEffect.Target target, double amount, AttributeModifier.Operation operation);

    /** Registers a {@code pipican:on} reaction for this player. */
    void addReaction(ReactionEffect reaction);

    /** Changes an attribute of the player's eidolon. */
    void addEidolonUpgrade(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation);

    /** Bleeding on crits (or every hit); values add up over effects, {@code maxStacks} takes the highest. */
    void addBleed(double damage, double seconds, int maxStacks, boolean anyHit, boolean slows, Optional<HolderSet<Item>> weapons);

    /** Changes the chance of reactions named {@code name}. */
    void addReactionChance(String name, double amount, double multiplier, double uses);

    /** A chance to avoid hits from arrows and other projectiles. */
    void addRangedDodge(double chance);

    /** Hits that leave a mob below {@code threshold} of its health kill it (only {@code victims}, if given). */
    void addExecute(double threshold, Optional<HolderSet<EntityType<?>>> victims);

    /** Nearby players (and you, with {@code includeSelf}) get {@code effects}. */
    void addAura(String name, double radius, boolean includeSelf, List<ClassEffect> effects);

    /** Upgrades the auras whose name contains {@code name} (all, if empty). */
    void addAuraMod(String name, double radius, double strength, boolean includeSelf, List<ClassEffect> effects);

    /** Upgrades for the player's summons. */
    void addSummonMod(double damage, double health, double seconds, int count, double hearts);

    /** Upgrades for the player's tamed animals. */
    void addPetMod(com.pipican.player.ClassState.PetMods mods);

    /** Changes to an effect the player gives others. */
    void addAppliedRule(Holder<MobEffect> effect, com.pipican.player.ClassState.AppliedRule rule);

    /** {@code effect} lasts {@code multiplier} times as long on the player. */
    void addEffectDuration(Holder<MobEffect> effect, double multiplier);

    /** Upgrades the reactions called {@code name}. */
    void addReactionMod(String name, com.pipican.player.ClassState.ReactionMod mod);

    /** Upgrades one ability. */
    void addAbilityMod(net.minecraft.resources.ResourceKey<com.pipican.ability.Ability> ability, com.pipican.player.ClassState.AbilityMod mod);

    /** Some mobs never target the player. */
    void addIgnoredBy(com.pipican.effect.type.IgnoredByEffect ignore);

    /**
     * Applies effects at {@code strength} (aura upgrades: % bonuses and attribute amounts are scaled), with
     * {@link Scaling} sources read from {@code source} (the aura owner) when it isn't null.
     */
    default void scaled(double strength, @Nullable ToDoubleFunction<String> source, Runnable apply) {
        apply.run();
    }

    /** The player picks a favored weapon type when they wake up. */
    void enableFavoredWeapon();

    /** Looking at a mob for {@code lookSeconds} marks it for {@code seconds}. */
    void addScrutiny(double lookSeconds, double seconds);

    /** Melee hits also hit nearby mobs (values add up; max targets takes the highest). */
    void addCleave(double fraction, double radius, int maxTargets, boolean chargedOnly, Optional<HolderSet<Item>> weapons);

    /** Changes an attribute of whatever the player rides. */
    void addMountUpgrade(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation);

    /** Spell points that refill on every sleep. */
    void addSpellPoints(int amount);

    /** Lets the player fly (creative-style) while this effect counts. */
    void grantFlight();

    /** Tests a condition now, and remembers it so the bonuses are rebuilt when its answer changes. */
    boolean test(ClassCondition condition);
}

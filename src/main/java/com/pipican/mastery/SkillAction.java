package com.pipican.mastery;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Something the player just did that may count as "using" a skill. Event handlers create these
 * and hand them to {@link com.pipican.player.MasteryService#onAction}; each skill's {@link SkillTrigger} decides
 * whether it matches.
 *
 * <p>To support a new kind of action (in code): add a record here, fire it from an event handler,
 * and add a trigger type that reacts to it.
 */
public sealed interface SkillAction {

    /** Item used for the action, for affinity-scaled mastery gain. Empty if none. */
    default ItemStack item() {
        return ItemStack.EMPTY;
    }

    /** Continuous actions (movement) are not limited by a skill's cooldown. */
    default boolean continuous() {
        return false;
    }

    /** Player hit a living mob in melee with {@code item}. */
    record MeleeHit(ItemStack item) implements SkillAction {}

    /** A projectile fired from {@code item} hit a living mob. */
    record RangedHit(ItemStack item) implements SkillAction {}

    /** Player killed a mob; {@code item} is the weapon (may be empty). */
    record Kill(ItemStack item) implements SkillAction {}

    /** Player blocked an attack with {@code item} (a shield). */
    record Block(ItemStack item) implements SkillAction {}

    /** Player took damage while wearing {@code armor} (the four armor slots, empty ones skipped). */
    record HurtWearing(List<ItemStack> armor) implements SkillAction {}

    /** Player moved {@code distance} blocks this tick in {@code mode}. */
    record Moved(MoveMode mode, double distance) implements SkillAction {
        @Override
        public boolean continuous() {
            return true;
        }
    }

    /** Player jumped. */
    record Jumped() implements SkillAction {}

    /** An active ability was used (fired by the future ability system, e.g. a dodge roll). */
    record AbilityUsed(Identifier ability, ItemStack item) implements SkillAction {}
}

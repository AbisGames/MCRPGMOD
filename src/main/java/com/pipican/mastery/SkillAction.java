package com.pipican.mastery;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Optional;
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

    /** The mob hit or killed, if any (for victim filters such as "killing undead"). */
    default Optional<LivingEntity> target() {
        return Optional.empty();
    }

    /** Damage dealt or taken, resource spent, health healed... (0 if it doesn't apply). */
    default double amount() {
        return 0;
    }

    /** Continuous actions (movement) are not limited by a skill's cooldown. */
    default boolean continuous() {
        return false;
    }

    /** Player hit a living mob in melee with {@code item}. */
    record MeleeHit(ItemStack item, Optional<LivingEntity> target, double amount) implements SkillAction {
        public MeleeHit(ItemStack item) {
            this(item, Optional.empty(), 0);
        }
    }

    /** A projectile fired from {@code item} hit a living mob. */
    record RangedHit(ItemStack item, Optional<LivingEntity> target, double amount) implements SkillAction {
        public RangedHit(ItemStack item) {
            this(item, Optional.empty(), 0);
        }
    }

    /** Player killed a mob; {@code item} is the weapon (may be empty). */
    record Kill(ItemStack item, Optional<LivingEntity> target) implements SkillAction {
        public Kill(ItemStack item) {
            this(item, Optional.empty());
        }
    }

    /** Player blocked an attack with {@code item} (a shield). */
    record Block(ItemStack item) implements SkillAction {}

    /** Player took damage while wearing {@code armor} (the four armor slots, empty ones skipped). */
    record HurtWearing(List<ItemStack> armor, double amount) implements SkillAction {
        public HurtWearing(List<ItemStack> armor) {
            this(armor, 0);
        }
    }

    /** Player moved {@code distance} blocks this tick in {@code mode}. */
    record Moved(MoveMode mode, double distance) implements SkillAction {
        @Override
        public boolean continuous() {
            return true;
        }
    }

    /** Player jumped. */
    record Jumped() implements SkillAction {}

    /**
     * Everything else a skill can train from ({@code pipican:did}): {@code item} / {@code block} / {@code target} /
     * {@code amount} are filled in where they make sense (the crafted item, the mined block, the healed amount...).
     */
    record Did(Kind kind, ItemStack item, Optional<BlockState> block, Optional<LivingEntity> target, double amount) implements SkillAction {

        public static Did of(Kind kind) {
            return new Did(kind, ItemStack.EMPTY, Optional.empty(), Optional.empty(), 0);
        }

        public enum Kind implements StringRepresentable {
            CRIT, BLEED, DODGE, SPEND, HEAL, TELEPORT, ENCHANT, BREW, CRAFT, SMELT, TRADE, TAME, BREED, MINE, HARVEST, PLANT,
            BONE_MEAL, ANVIL, LEVEL_UP, DRINK, PET_KILL, FISH, TAKE_FALL, HURT,
            HEAL_OTHER, EXECUTE, CHARM, EXPLOSION_HIT, ACTIVE_SECOND, POISON, USE_ITEM, ABSORB;

            public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }
    }

    /** A named {@code pipican:on} reaction fired ("landing Trickery hits", "triggering Seize Chaos"). */
    record Reacted(String name) implements SkillAction {}

    /** An active ability was used (fired by the future ability system, e.g. a dodge roll). */
    record AbilityUsed(Identifier ability, ItemStack item) implements SkillAction {}
}

package com.pipican.core;

import com.pipican.rpgclass.Affinity;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config ({@code world/serverconfig/pipican-server.toml}). Holds balance numbers that are
 * the same for every class: the XP curves and what each affinity tier does.
 * Class-specific numbers belong in the class JSON instead.
 */
public final class PipicanConfig {

    /** What an {@link Affinity} tier does. All values are multipliers (1.0 = no change). */
    public record TierValues(double damage, double attackSpeed, double armor, double movePenaltyPerArmorPiece, double trinket, double mastery) {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue XP_BASE;
    private static final ModConfigSpec.DoubleValue XP_EXPONENT;
    private static final ModConfigSpec.DoubleValue KILL_XP_PER_MAX_HEALTH;
    private static final ModConfigSpec.LongValue STAT_FIRST_POINT_XP;
    private static final ModConfigSpec.LongValue STAT_POINT_XP_STEP;
    private static final ModConfigSpec.BooleanValue OPEN_SELECTION_ON_JOIN;
    private static final ModConfigSpec.IntValue REFUND_COST;
    private static final Map<Affinity, TierSpec> TIERS = new EnumMap<>(Affinity.class);

    private record TierSpec(ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue attackSpeed, ModConfigSpec.DoubleValue armor,
                            ModConfigSpec.DoubleValue movePenalty, ModConfigSpec.DoubleValue trinket, ModConfigSpec.DoubleValue mastery,
                            TierValues defaults) {}

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Class levelling. Class XP is separate from vanilla XP.").push("leveling");
        XP_BASE = b.comment("XP needed to go from level L to L+1 is base * L^exponent.")
                .defineInRange("xp_base", 100.0, 1.0, 1_000_000.0);
        XP_EXPONENT = b.defineInRange("xp_exponent", 1.5, 0.0, 10.0);
        KILL_XP_PER_MAX_HEALTH = b.comment("Kill XP gained per point of max health of a killed mob. Kill XP counts towards",
                        "both the class level and stat points.")
                .defineInRange("kill_xp_per_max_health", 0.5, 0.0, 1000.0);
        b.pop();

        b.comment("Stat points are earned with lifetime kill XP (vanilla XP is not used).",
                "The Nth point needs first_point_xp + (N - 1) * xp_step more XP than the one before it.").push("stats");
        STAT_FIRST_POINT_XP = b.comment("Kill XP needed for the first stat point.")
                .defineInRange("first_point_xp", 100L, 1L, 1_000_000_000L);
        STAT_POINT_XP_STEP = b.comment("How much more XP each further stat point needs than the previous one (0 = flat).")
                .defineInRange("xp_step", 50L, 0L, 1_000_000_000L);
        b.pop();

        b.push("skill_tree");
        REFUND_COST = b.comment("Vanilla XP levels it costs to refund one passive-tree node (free in creative).")
                .defineInRange("refund_cost_levels", 1, 0, 1000);
        b.pop();

        b.push("selection");
        OPEN_SELECTION_ON_JOIN = b.comment("Open the class-selection screen for class-less players when they join.")
                .define("open_on_join", true);
        b.pop();

        b.comment("What each affinity tier does. Every value is a multiplier (1.0 = unchanged).",
                "damage: damage dealt with a weapon of this tier (melee and projectiles).",
                "attack_speed: attack speed while holding it.",
                "armor: armor and toughness given by an armor piece of this tier.",
                "move_penalty: movement speed multiplier per worn armor piece of this tier.",
                "trinket: strength of the attribute bonuses of a trinket of this tier.",
                "mastery: skill mastery gained when using an item of this tier.").push("affinity");
        defineTier(b, Affinity.MASTERY, new TierValues(1.25, 1.10, 1.15, 1.00, 1.25, 1.50));
        defineTier(b, Affinity.PROFICIENT, new TierValues(1.10, 1.05, 1.05, 1.00, 1.10, 1.25));
        defineTier(b, Affinity.NEUTRAL, new TierValues(1.00, 1.00, 1.00, 1.00, 1.00, 1.00));
        defineTier(b, Affinity.CLUMSY, new TierValues(0.75, 0.85, 0.75, 0.97, 0.75, 0.75));
        defineTier(b, Affinity.FORBIDDEN, new TierValues(0.35, 0.60, 0.40, 0.90, 0.25, 0.50));
        b.pop();

        SPEC = b.build();
    }

    private static void defineTier(ModConfigSpec.Builder b, Affinity tier, TierValues d) {
        b.push(tier.getSerializedName());
        TIERS.put(tier, new TierSpec(
                b.defineInRange("damage", d.damage(), 0.0, 10.0),
                b.defineInRange("attack_speed", d.attackSpeed(), 0.0, 10.0),
                b.defineInRange("armor", d.armor(), 0.0, 10.0),
                b.defineInRange("move_penalty", d.movePenaltyPerArmorPiece(), 0.0, 10.0),
                b.defineInRange("trinket", d.trinket(), 0.0, 10.0),
                b.defineInRange("mastery", d.mastery(), 0.0, 10.0),
                d));
        b.pop();
    }

    private PipicanConfig() {}

    public static TierValues tier(Affinity affinity) {
        TierSpec s = TIERS.get(affinity);
        if (!SPEC.isLoaded()) {
            return s.defaults();
        }
        return new TierValues(s.damage.get(), s.attackSpeed.get(), s.armor.get(), s.movePenalty.get(), s.trinket.get(), s.mastery.get());
    }

    /** Class XP needed to advance from {@code level} to {@code level + 1}. */
    public static long xpToNextLevel(int level) {
        double base = SPEC.isLoaded() ? XP_BASE.get() : 100.0;
        double exp = SPEC.isLoaded() ? XP_EXPONENT.get() : 1.5;
        return Math.max(1, Math.round(base * Math.pow(level, exp)));
    }

    public static double killXpPerMaxHealth() {
        return SPEC.isLoaded() ? KILL_XP_PER_MAX_HEALTH.get() : 0.5;
    }

    public static long statFirstPointXp() {
        return SPEC.isLoaded() ? STAT_FIRST_POINT_XP.get() : 100L;
    }

    public static long statPointXpStep() {
        return SPEC.isLoaded() ? STAT_POINT_XP_STEP.get() : 50L;
    }

    public static int refundCostLevels() {
        return SPEC.isLoaded() ? REFUND_COST.get() : 1;
    }

    public static boolean openSelectionOnJoin() {
        return !SPEC.isLoaded() || OPEN_SELECTION_ON_JOIN.get();
    }
}

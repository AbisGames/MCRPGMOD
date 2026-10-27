package com.pipican.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Ability;
import com.pipican.core.PipicanRegistries;
import com.pipican.mastery.Skill;
import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatDefinition;
import com.pipican.stat.StatPoints;
import com.pipican.trinket.TrinketSlotType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the class system saves on a player. Attached via
 * {@link PipicanAttachments#CLASS_DATA}, saved with the player, kept through death and synced to
 * the owning client only.
 *
 * <p>Only <i>choices</i> are stored here (class, levels, nodes, bought stats, stat XP, trinkets). Derived
 * numbers - total stats, attribute modifiers, damage multipliers - are recomputed from the JSON
 * data by {@link ClassEffectApplier}, so editing a class JSON changes existing players too.
 *
 * <p>Always mutate through {@link ClassService}, which validates, syncs and refreshes bonuses.
 */
public final class PlayerClassData {

    public static final MapCodec<PlayerClassData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.CLASS).optionalFieldOf("active_class").forGetter(d -> d.activeClass),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.CLASS), ClassProgress.CODEC).optionalFieldOf("progress", Map.of()).forGetter(d -> d.progress),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.STAT), Codec.INT).optionalFieldOf("bought_stats", Map.of()).forGetter(d -> d.boughtStats),
            Codec.LONG.optionalFieldOf("stat_xp", 0L).forGetter(d -> d.statXp),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.TRINKET_SLOT), ItemStack.OPTIONAL_CODEC.listOf()).optionalFieldOf("trinkets", Map.of()).forGetter(d -> d.trinkets),
            Codec.DOUBLE.optionalFieldOf("resource", 0.0).forGetter(d -> d.resource),
            Codec.DOUBLE.optionalFieldOf("resource_max", 0.0).forGetter(d -> d.resourceMax),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.SKILL), Codec.DOUBLE).optionalFieldOf("mastery", Map.of()).forGetter(d -> d.mastery),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.ABILITY), Codec.LONG).optionalFieldOf("ability_cooldowns", Map.of()).forGetter(d -> d.abilityCooldowns),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.ABILITY), Codec.LONG).optionalFieldOf("ability_active", Map.of()).forGetter(d -> d.abilityActive),
            Extras.MAP_CODEC.forGetter(d -> d.extras)
    ).apply(i, PlayerClassData::new));

    /**
     * Rarely used class mechanics (grouped only because a codec takes at most 16 fields):
     * Recall's waypoint, abilities used since the last sleep, spell points, the Lasting Bond partner
     * and the Optimization choice.
     */
    public static final class Extras {
        static final MapCodec<Extras> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                GlobalPos.CODEC.optionalFieldOf("waypoint").forGetter(e -> e.waypoint),
                Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.ABILITY), Codec.INT).optionalFieldOf("rest_uses", Map.of()).forGetter(e -> e.restUses),
                Codec.INT.optionalFieldOf("spell_points", 0).forGetter(e -> e.spellPoints),
                UUIDUtil.CODEC.optionalFieldOf("bond").forGetter(e -> e.bond),
                Optimization.CODEC.optionalFieldOf("optimization").forGetter(e -> e.optimization)
        ).apply(i, Extras::new));

        public Optional<GlobalPos> waypoint;
        public final Map<ResourceKey<Ability>, Integer> restUses;
        public int spellPoints;
        public Optional<UUID> bond;
        public Optional<Optimization> optimization;

        Extras(Optional<GlobalPos> waypoint, Map<ResourceKey<Ability>, Integer> restUses, int spellPoints, Optional<UUID> bond,
               Optional<Optimization> optimization) {
            this.waypoint = waypoint;
            this.restUses = new HashMap<>(restUses);
            this.spellPoints = spellPoints;
            this.bond = bond;
            this.optimization = optimization;
        }

        Extras() {
            this(Optional.empty(), Map.of(), 0, Optional.empty(), Optional.empty());
        }
    }

    /** Transmuter's Optimization: one stat raised, one lowered, until the next sleep. {@code pending}: a choice is owed. */
    public record Optimization(Optional<ResourceKey<StatDefinition>> raise, Optional<ResourceKey<StatDefinition>> lower, boolean pending) {
        public static final Codec<Optimization> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(PipicanRegistries.STAT).optionalFieldOf("raise").forGetter(Optimization::raise),
                ResourceKey.codec(PipicanRegistries.STAT).optionalFieldOf("lower").forGetter(Optimization::lower),
                Codec.BOOL.optionalFieldOf("pending", false).forGetter(Optimization::pending)
        ).apply(i, Optimization::new));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerClassData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());

    private Optional<ResourceKey<RpgClass>> activeClass;
    private final Map<ResourceKey<RpgClass>, ClassProgress> progress;
    /** Stat points put directly into stats. Belong to the player, not a class. */
    private final Map<ResourceKey<StatDefinition>, Integer> boughtStats;
    /** Lifetime kill XP. Never resets; earned stat points are derived from it (see {@link StatPoints}). */
    private long statXp;
    private final Map<ResourceKey<TrinketSlotType>, List<ItemStack>> trinkets;
    /** Current amount of the special-stat resource (mana, rage...). */
    private double resource;
    /** Cached max of the resource, computed server-side and synced for the HUD. */
    private double resourceMax;
    /** Mastery points per skill. Belongs to the player (survives class changes). Tiers are derived from it. */
    private final Map<ResourceKey<Skill>, Double> mastery;
    /** Game time at which each ability is off cooldown again. */
    private final Map<ResourceKey<Ability>, Long> abilityCooldowns;
    /** Running ability states and the game time they end ({@link Long#MAX_VALUE} for toggles). */
    private final Map<ResourceKey<Ability>, Long> abilityActive;
    private final Extras extras;
    /** Not saved: set when mastery changed but the client hasn't been told yet (synced in batches). */
    private boolean masteryDirty;

    public PlayerClassData() {
        this(Optional.empty(), Map.of(), Map.of(), 0L, Map.of(), 0, 0, Map.of(), Map.of(), Map.of(), new Extras());
    }

    private PlayerClassData(Optional<ResourceKey<RpgClass>> activeClass, Map<ResourceKey<RpgClass>, ClassProgress> progress,
                            Map<ResourceKey<StatDefinition>, Integer> boughtStats, long statXp,
                            Map<ResourceKey<TrinketSlotType>, List<ItemStack>> trinkets, double resource, double resourceMax,
                            Map<ResourceKey<Skill>, Double> mastery, Map<ResourceKey<Ability>, Long> abilityCooldowns,
                            Map<ResourceKey<Ability>, Long> abilityActive, Extras extras) {
        this.activeClass = activeClass;
        this.progress = new HashMap<>(progress);
        this.boughtStats = new HashMap<>(boughtStats);
        this.statXp = statXp;
        this.trinkets = new HashMap<>();
        trinkets.forEach((k, v) -> this.trinkets.put(k, new ArrayList<>(v)));
        this.resource = resource;
        this.resourceMax = resourceMax;
        this.mastery = new HashMap<>(mastery);
        this.abilityCooldowns = new HashMap<>(abilityCooldowns);
        this.abilityActive = new HashMap<>(abilityActive);
        this.extras = extras;
    }

    public Optional<ResourceKey<RpgClass>> activeClass() {
        return activeClass;
    }

    public boolean hasClass() {
        return activeClass.isPresent();
    }

    void setActiveClass(Optional<ResourceKey<RpgClass>> activeClass) {
        this.activeClass = activeClass;
    }

    /** Progress in the active class, created on demand. Empty if the player has no class. */
    public Optional<ClassProgress> activeProgress() {
        return activeClass.map(key -> progress.computeIfAbsent(key, k -> new ClassProgress()));
    }

    Map<ResourceKey<RpgClass>, ClassProgress> allProgress() {
        return progress;
    }

    /** Read-only view of the progress in every class the player has played. */
    public Map<ResourceKey<RpgClass>, ClassProgress> progressByClass() {
        return Collections.unmodifiableMap(progress);
    }

    public int boughtStat(ResourceKey<StatDefinition> stat) {
        return boughtStats.getOrDefault(stat, 0);
    }

    public int totalBoughtStats() {
        return boughtStats.values().stream().mapToInt(Integer::intValue).sum();
    }

    Map<ResourceKey<StatDefinition>, Integer> boughtStats() {
        return boughtStats;
    }

    public long statXp() {
        return statXp;
    }

    void setStatXp(long statXp) {
        this.statXp = statXp;
    }

    /** Mutable list of the stacks in one trinket slot type (one entry per slot). */
    public List<ItemStack> trinkets(ResourceKey<TrinketSlotType> slot) {
        return trinkets.computeIfAbsent(slot, k -> new ArrayList<>());
    }

    public Map<ResourceKey<TrinketSlotType>, List<ItemStack>> allTrinkets() {
        return trinkets;
    }

    public double resource() {
        return resource;
    }

    public void setResource(double resource) {
        this.resource = resource;
    }

    public double resourceMax() {
        return resourceMax;
    }

    void setResourceMax(double resourceMax) {
        this.resourceMax = resourceMax;
    }

    public double mastery(ResourceKey<Skill> skill) {
        return mastery.getOrDefault(skill, 0.0);
    }

    Map<ResourceKey<Skill>, Double> allMastery() {
        return mastery;
    }

    /** Game time the ability is ready again (0 if never used). */
    public long abilityReadyAt(ResourceKey<Ability> ability) {
        return abilityCooldowns.getOrDefault(ability, 0L);
    }

    /** Game time a running ability state ends, if it is running. */
    public Optional<Long> abilityActiveUntil(ResourceKey<Ability> ability) {
        return Optional.ofNullable(abilityActive.get(ability));
    }

    /** Mutable: ability -> ready-at game time. Mutate through AbilityService. */
    public Map<ResourceKey<Ability>, Long> abilityCooldowns() {
        return abilityCooldowns;
    }

    /** Mutable: running ability -> end game time. Mutate through AbilityService. */
    public Map<ResourceKey<Ability>, Long> abilityActive() {
        return abilityActive;
    }

    /** Waypoint, rest uses, spell points, bond, Optimization. Mutate through the services. */
    public Extras extras() {
        return extras;
    }

    boolean masteryDirty() {
        return masteryDirty;
    }

    void setMasteryDirty(boolean masteryDirty) {
        this.masteryDirty = masteryDirty;
    }
}

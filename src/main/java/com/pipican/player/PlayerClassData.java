package com.pipican.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.core.PipicanRegistries;
import com.pipican.mastery.Skill;
import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketSlotType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * <p>Only <i>choices</i> are stored here (class, levels, nodes, bought stats, trinkets). Derived
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
            Codec.INT.optionalFieldOf("stat_levels_spent", 0).forGetter(d -> d.statLevelsSpent),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.TRINKET_SLOT), ItemStack.OPTIONAL_CODEC.listOf()).optionalFieldOf("trinkets", Map.of()).forGetter(d -> d.trinkets),
            Codec.DOUBLE.optionalFieldOf("resource", 0.0).forGetter(d -> d.resource),
            Codec.DOUBLE.optionalFieldOf("resource_max", 0.0).forGetter(d -> d.resourceMax),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.SKILL), Codec.DOUBLE).optionalFieldOf("mastery", Map.of()).forGetter(d -> d.mastery),
            Codec.BOOL.optionalFieldOf("trainer_mode", false).forGetter(d -> d.trainerMode),
            ResourceKey.codec(PipicanRegistries.CLASS).optionalFieldOf("class_before_training").forGetter(d -> d.classBeforeTraining)
    ).apply(i, PlayerClassData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerClassData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());

    private Optional<ResourceKey<RpgClass>> activeClass;
    private final Map<ResourceKey<RpgClass>, ClassProgress> progress;
    /** Stat points bought with vanilla XP. Belong to the player, not a class. */
    private final Map<ResourceKey<StatDefinition>, Integer> boughtStats;
    /** Vanilla levels spent on stats, so a stat reset can refund them. */
    private int statLevelsSpent;
    private final Map<ResourceKey<TrinketSlotType>, List<ItemStack>> trinkets;
    /** Current amount of the special-stat resource (mana, rage...). */
    private double resource;
    /** Cached max of the resource, computed server-side and synced for the HUD. */
    private double resourceMax;
    /** Mastery points per skill. Belongs to the player (survives class changes). Tiers are derived from it. */
    private final Map<ResourceKey<Skill>, Double> mastery;
    /** Not saved: set when mastery changed but the client hasn't been told yet (synced in batches). */
    private boolean masteryDirty;
    /** Persisted training lock; the class is disabled even after reconnect/respawn. */
    private boolean trainerMode;
    private Optional<ResourceKey<RpgClass>> classBeforeTraining;

    public PlayerClassData() {
        this(Optional.empty(), Map.of(), Map.of(), 0, Map.of(), 0, 0, Map.of(), false, Optional.empty());
    }

    private PlayerClassData(Optional<ResourceKey<RpgClass>> activeClass, Map<ResourceKey<RpgClass>, ClassProgress> progress,
                            Map<ResourceKey<StatDefinition>, Integer> boughtStats, int statLevelsSpent,
                            Map<ResourceKey<TrinketSlotType>, List<ItemStack>> trinkets, double resource, double resourceMax,
                            Map<ResourceKey<Skill>, Double> mastery, boolean trainerMode,
                            Optional<ResourceKey<RpgClass>> classBeforeTraining) {
        this.activeClass = activeClass;
        this.progress = new HashMap<>(progress);
        this.boughtStats = new HashMap<>(boughtStats);
        this.statLevelsSpent = statLevelsSpent;
        this.trinkets = new HashMap<>();
        trinkets.forEach((k, v) -> this.trinkets.put(k, new ArrayList<>(v)));
        this.resource = resource;
        this.resourceMax = resourceMax;
        this.mastery = new HashMap<>(mastery);
        this.trainerMode = trainerMode;
        this.classBeforeTraining = classBeforeTraining;
    }

    public boolean trainerMode() { return trainerMode; }
    public Optional<ResourceKey<RpgClass>> classBeforeTraining() { return classBeforeTraining; }
    void beginTraining() {
        if (trainerMode) throw new IllegalStateException("training already active");
        classBeforeTraining = activeClass;
        activeClass = Optional.empty();
        trainerMode = true;
    }
    void endTraining() {
        if (!trainerMode) throw new IllegalStateException("not training");
        activeClass = classBeforeTraining;
        classBeforeTraining = Optional.empty();
        trainerMode = false;
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

    public int boughtStat(ResourceKey<StatDefinition> stat) {
        return boughtStats.getOrDefault(stat, 0);
    }

    public int totalBoughtStats() {
        return boughtStats.values().stream().mapToInt(Integer::intValue).sum();
    }

    Map<ResourceKey<StatDefinition>, Integer> boughtStats() {
        return boughtStats;
    }

    public int statLevelsSpent() {
        return statLevelsSpent;
    }

    void setStatLevelsSpent(int statLevelsSpent) {
        this.statLevelsSpent = statLevelsSpent;
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

    boolean masteryDirty() {
        return masteryDirty;
    }

    void setMasteryDirty(boolean masteryDirty) {
        this.masteryDirty = masteryDirty;
    }
}

package com.pipican.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.skill.TreeKind;
import java.util.HashMap;
import java.util.Map;

/**
 * A player's progress in one class: class level, class XP and unlocked skill-node ranks.
 *
 * <p>Kept per class (inside {@link PlayerClassData}) so a future multiclass system can store
 * progress for several classes at once without changing the save format.
 */
public final class ClassProgress {

    public static final Codec<ClassProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(p -> p.level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(p -> p.xp),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("general_nodes", Map.of()).forGetter(p -> p.generalNodes),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("special_nodes", Map.of()).forGetter(p -> p.specialNodes)
    ).apply(i, ClassProgress::new));

    private int level;
    /** XP towards the next level (resets on level-up). */
    private long xp;
    private final Map<String, Integer> generalNodes;
    private final Map<String, Integer> specialNodes;

    public ClassProgress() {
        this(1, 0, Map.of(), Map.of());
    }

    private ClassProgress(int level, long xp, Map<String, Integer> generalNodes, Map<String, Integer> specialNodes) {
        this.level = level;
        this.xp = xp;
        this.generalNodes = new HashMap<>(generalNodes);
        this.specialNodes = new HashMap<>(specialNodes);
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long xp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    /** Node id -> unlocked rank for the given tree. Mutable. */
    public Map<String, Integer> nodes(TreeKind kind) {
        return kind == TreeKind.GENERAL ? generalNodes : specialNodes;
    }

    public int rank(TreeKind kind, String nodeId) {
        return nodes(kind).getOrDefault(nodeId, 0);
    }
}

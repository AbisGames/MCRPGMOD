package com.pipican.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.skill.TreeKind;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A player's progress in one class: class level, class XP and allocated passive-tree nodes.
 *
 * <p>Kept per class (inside {@link PlayerClassData}) so a future multiclass system can store
 * progress for several classes at once without changing the save format.
 */
public final class ClassProgress {

    public static final Codec<ClassProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(p -> p.level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(p -> p.xp),
            Codec.STRING.listOf().optionalFieldOf("general_nodes", List.of()).forGetter(p -> List.copyOf(p.generalNodes)),
            Codec.STRING.listOf().optionalFieldOf("special_nodes", List.of()).forGetter(p -> List.copyOf(p.specialNodes))
    ).apply(i, ClassProgress::new));

    private int level;
    /** XP towards the next level (resets on level-up). */
    private long xp;
    private final Set<String> generalNodes;
    private final Set<String> specialNodes;

    public ClassProgress() {
        this(1, 0, List.of(), List.of());
    }

    private ClassProgress(int level, long xp, List<String> generalNodes, List<String> specialNodes) {
        this.level = level;
        this.xp = xp;
        this.generalNodes = new HashSet<>(generalNodes);
        this.specialNodes = new HashSet<>(specialNodes);
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

    /** Allocated node ids in the given tree (the free start node is not included). Mutable. */
    public Set<String> nodes(TreeKind kind) {
        return kind == TreeKind.GENERAL ? generalNodes : specialNodes;
    }
}

package com.pipican.skill;

import com.pipican.core.ClassLookup;
import com.pipican.mastery.Skill;
import com.pipican.player.ClassProgress;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatPoints;
import net.minecraft.core.RegistryAccess;

/**
 * Skill-point arithmetic, shared by the server (which enforces it) and the tree screen (which
 * shows it). Points are never stored: earned comes from mastery, spent from allocated nodes.
 */
public final class TreePoints {

    private TreePoints() {}

    /**
     * Skill points earned for a mastery-fed tree (general or special): the class's starting points
     * plus one per mastery tier reached in every skill that feeds this tree ({@code points_to}) and
     * is allowed for the class. Not used for the stat tree.
     */
    public static int earned(RegistryAccess access, PlayerClassData data, RpgClass rpgClass, TreeKind kind) {
        int points = rpgClass.startingPoints(kind);
        for (var entry : ClassLookup.skills(access).entrySet()) {
            Skill skill = entry.getValue();
            if (skill.pointsTo() == kind && skill.allowedFor(data.activeClass())) {
                points += skill.tierFor(data.mastery(entry.getKey()));
            }
        }
        return points;
    }

    /** Points spent on allocated nodes. Nodes that no longer exist in the JSON cost nothing. */
    public static int spent(SkillTree tree, ClassProgress progress, TreeKind kind) {
        int spent = 0;
        for (String id : progress.nodes(kind)) {
            spent += tree.node(id).map(SkillNode::cost).orElse(0);
        }
        return spent;
    }

    /** Unspent points for a tree. The stat tree shares its pool with stats (see {@link StatPoints}). */
    public static int available(RegistryAccess access, PlayerClassData data, RpgClass rpgClass, TreeKind kind) {
        if (kind == TreeKind.STAT) {
            return StatPoints.available(access, data);
        }
        ClassProgress progress = data.activeProgress().orElse(null);
        if (progress == null) {
            return 0;
        }
        int spent = ClassLookup.tree(access, rpgClass, kind).map(t -> spent(t, progress, kind)).orElse(0);
        return earned(access, data, rpgClass, kind) - spent;
    }
}

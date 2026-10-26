package com.pipican.skill;

import com.pipican.core.ClassLookup;
import com.pipican.mastery.Skill;
import com.pipican.player.ClassProgress;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import net.minecraft.core.RegistryAccess;

/**
 * Skill-point arithmetic, shared by the server (which enforces it) and the tree screen (which
 * shows it). Points are never stored: earned comes from mastery, spent from allocated nodes.
 */
public final class TreePoints {

    private TreePoints() {}

    /**
     * Points earned for a tree: the class's starting points plus one per mastery tier reached in
     * every skill that feeds this tree ({@code points_to}) and is allowed for the class.
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

    public static int available(RegistryAccess access, PlayerClassData data, RpgClass rpgClass, TreeKind kind) {
        ClassProgress progress = data.activeProgress().orElse(null);
        if (progress == null) {
            return 0;
        }
        int spent = ClassLookup.tree(access, rpgClass, kind).map(t -> spent(t, progress, kind)).orElse(0);
        return earned(access, data, rpgClass, kind) - spent;
    }
}

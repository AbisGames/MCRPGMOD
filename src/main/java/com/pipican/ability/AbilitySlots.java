package com.pipican.ability;

import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanRegistries;
import com.pipican.player.ClassProgress;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.TreeKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Which abilities a player has, and which four sit on the ability keys. Shared by the server (which
 * enforces it) and the HUD (which shows it), so both always agree.
 */
public final class AbilitySlots {

    public static final int SLOTS = 4;

    private AbilitySlots() {}

    /** The class's own abilities first, then abilities granted by owned tree nodes (in tree order) and reached mastery tiers. */
    public static List<ResourceKey<Ability>> granted(RegistryAccess access, PlayerClassData data) {
        Optional<RpgClass> cls = data.activeClass().flatMap(k -> ClassLookup.rpgClass(access, k));
        Optional<ClassProgress> progress = data.activeProgress();
        if (cls.isEmpty() || progress.isEmpty()) {
            return List.of();
        }
        var registry = ClassLookup.abilities(access);
        Set<ResourceKey<Ability>> out = new LinkedHashSet<>(cls.get().abilities());
        for (TreeKind kind : TreeKind.values()) {
            var tree = ClassLookup.tree(access, cls.get(), kind);
            if (tree.isEmpty()) {
                continue;
            }
            Set<String> owned = new HashSet<>(progress.get().nodes(kind));
            cls.get().tree(kind).ifPresent(ref -> owned.add(ref.start()));
            for (var node : tree.get().nodes()) {
                if (owned.contains(node.id())) {
                    for (Identifier id : node.grants()) {
                        out.add(ResourceKey.create(PipicanRegistries.ABILITY, id));
                    }
                }
            }
        }
        // Skill mastery tiers can unlock abilities too.
        for (var entry : ClassLookup.skills(access).entrySet()) {
            var skill = entry.getValue();
            if (!skill.allowedFor(data.activeClass())) {
                continue;
            }
            int tier = skill.tierFor(data.mastery(entry.getKey()));
            for (int t = 0; t < Math.min(tier, skill.tiers().size()); t++) {
                for (Identifier id : skill.tiers().get(t).grants()) {
                    out.add(ResourceKey.create(PipicanRegistries.ABILITY, id));
                }
            }
        }
        out.removeIf(key -> !registry.containsKey(key));
        return List.copyOf(out);
    }

    /** The abilities on keys 1-4: granted ones that don't start by themselves ({@code auto}). */
    public static List<ResourceKey<Ability>> slots(RegistryAccess access, PlayerClassData data) {
        var registry = ClassLookup.abilities(access);
        List<ResourceKey<Ability>> out = new ArrayList<>();
        for (var key : granted(access, data)) {
            if (registry.getValue(key).auto().isEmpty() && out.size() < SLOTS) {
                out.add(key);
            }
        }
        return out;
    }
}

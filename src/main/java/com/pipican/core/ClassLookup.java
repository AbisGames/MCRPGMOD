package com.pipican.core;

import com.pipican.ability.Ability;
import com.pipican.mastery.Skill;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.SkillTree;
import com.pipican.skill.SkillTrees;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketSlotType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

/** Small helpers for reading the data-driven registries. Works on both server and client. */
public final class ClassLookup {

    private ClassLookup() {}

    public static Registry<RpgClass> classes(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.CLASS);
    }

    public static Registry<com.pipican.ore.GearSet> gearSets(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.GEAR_SET);
    }

    public static Registry<StatDefinition> stats(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.STAT);
    }

    public static Registry<Skill> skills(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.SKILL);
    }

    public static Registry<Ability> abilities(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.ABILITY);
    }

    public static Registry<TrinketSlotType> trinketSlots(RegistryAccess access) {
        return access.lookupOrThrow(PipicanRegistries.TRINKET_SLOT);
    }

    public static Optional<RpgClass> rpgClass(RegistryAccess access, ResourceKey<RpgClass> key) {
        return classes(access).getOptional(key);
    }

    public static Optional<SkillTree> tree(RegistryAccess access, RpgClass rpgClass, TreeKind kind) {
        return rpgClass.tree(kind).flatMap(ref -> SkillTrees.get(access, ref.tree()));
    }

    /** Classes players may pick on the selection screen, sorted by id for a stable order. */
    public static List<Map.Entry<ResourceKey<RpgClass>, RpgClass>> selectableClasses(RegistryAccess access) {
        return classes(access).entrySet().stream()
                .filter(e -> e.getValue().selectable())
                .sorted(Comparator.comparing(e -> e.getKey().identifier().toString()))
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }

    /** Trinket slots of a class with a count above zero, sorted by the slot type's {@code order}. */
    public static List<Map.Entry<ResourceKey<TrinketSlotType>, Integer>> trinketLayout(RegistryAccess access, RpgClass rpgClass) {
        Registry<TrinketSlotType> slots = trinketSlots(access);
        return rpgClass.trinketSlots().entrySet().stream()
                .filter(e -> e.getValue() > 0 && slots.containsKey(e.getKey()))
                .sorted(Comparator.<Map.Entry<ResourceKey<TrinketSlotType>, Integer>>comparingInt(e -> slots.getValue(e.getKey()).order())
                        .thenComparing(e -> e.getKey().identifier().toString()))
                .toList();
    }
}

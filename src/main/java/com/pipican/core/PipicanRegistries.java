package com.pipican.core;

import com.pipican.Pipican;
import com.pipican.mastery.Skill;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.SkillTreeFile;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketSlotType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The data-driven registries that make the class system modular.
 *
 * <p>Every entry is a JSON file in a datapack (the mod's own built-in data, or any server datapack):
 * <pre>
 *   data/&lt;namespace&gt;/pipican/stat/&lt;name&gt;.json          -> {@link StatDefinition}
 *   data/&lt;namespace&gt;/pipican/trinket_slot/&lt;name&gt;.json  -> {@link TrinketSlotType}
 *   data/&lt;namespace&gt;/pipican/skill_tree/&lt;name&gt;.json    -> {@link SkillTreeFile} (several files can form one tree)
 *   data/&lt;namespace&gt;/pipican/class/&lt;name&gt;.json         -> {@link RpgClass}
 *   data/&lt;namespace&gt;/pipican/skill/&lt;name&gt;.json         -> {@link Skill} (masterable skills)
 * </pre>
 * Adding a new class therefore never needs Java code - only new JSON files. See docs/CLASS_SYSTEM.md.
 *
 * <p>All registries are synced to clients so screens (class selection, trinkets) can read them.
 */
public final class PipicanRegistries {
    public static final ResourceKey<Registry<StatDefinition>> STAT = key("stat");
    public static final ResourceKey<Registry<TrinketSlotType>> TRINKET_SLOT = key("trinket_slot");
    public static final ResourceKey<Registry<SkillTreeFile>> SKILL_TREE = key("skill_tree");
    public static final ResourceKey<Registry<Skill>> SKILL = key("skill");
    public static final ResourceKey<Registry<RpgClass>> CLASS = key("class");

    private PipicanRegistries() {}

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(Pipican.id(name));
    }

    /** Mod-bus listener: tells NeoForge to load (and sync) our JSON registries. */
    public static void onNewDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(STAT, StatDefinition.CODEC, StatDefinition.CODEC);
        event.dataPackRegistry(TRINKET_SLOT, TrinketSlotType.CODEC, TrinketSlotType.CODEC);
        event.dataPackRegistry(SKILL_TREE, SkillTreeFile.CODEC, SkillTreeFile.CODEC);
        event.dataPackRegistry(SKILL, Skill.CODEC, Skill.CODEC);
        event.dataPackRegistry(CLASS, RpgClass.CODEC, RpgClass.CODEC);
    }
}

package com.pipican.resource;

import com.pipican.Pipican;
import com.pipican.boss.BossContent;
import com.pipican.ore.PipicanOres;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Rare materials. The eight rare deposits (soulstone, dragonite, abyssium, runestone, astralite, sunforged, moonshard,
 * primordial) are ores of the tiered catalog ({@link PipicanOres}): they spawn in their regions by tier and are mined
 * with a pickaxe of the right tier. The boss materials (infernium, voidsteel, celestium, bloodcrystal) live here.
 */
public final class RareResources {
    /** The deposits, by their material's name. */
    public static final List<String> DEPOSITS = List.of("soulstone", "dragonite", "abyssium", "runestone", "astralite", "sunforged", "moonshard", "primordial");
    /** End deposits are end-stone ores in the catalog. */
    private static final Map<String, String> ORE_BLOCK = Map.of("dragonite", "end_dragonite_ore", "primordial", "end_primordial_ore");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pipican.MOD_ID);
    public static final Map<String, DeferredBlock<Block>> ORES;
    public static final Map<String, DeferredItem<Item>> MATERIALS;

    static {
        Map<String, DeferredBlock<Block>> ores = new LinkedHashMap<>();
        Map<String, DeferredItem<Item>> materials = new LinkedHashMap<>();
        for (String deposit : DEPOSITS) {
            ores.put(deposit, PipicanOres.block(ORE_BLOCK.getOrDefault(deposit, deposit + "_ore")));
            materials.put(deposit, PipicanOres.material(deposit));
        }
        materials.put("infernium", BossContent.INFERNIUM);
        for (String name : List.of("voidsteel", "celestium", "bloodcrystal")) {
            materials.put(name, ITEMS.registerSimpleItem(name, properties -> properties.fireResistant()));
        }
        ORES = Collections.unmodifiableMap(ores);
        MATERIALS = Collections.unmodifiableMap(materials);
    }

    private RareResources() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

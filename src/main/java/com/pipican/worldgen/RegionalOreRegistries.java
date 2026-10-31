package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The 27 regions' mining nodes and materials, by regional ore id ({@link RegionalOreCatalog}). The blocks and items
 * themselves are the tiered ore catalog's ({@link com.pipican.ore.PipicanOres}): {@code NODES.get(id)} is the T1 ore
 * the region's renewable mine holds, {@code MATERIALS.get(id)} the region's signature material (an ingot or gem) that
 * its boss drops and its blacksmith uses. Only the regional boss cores are registered here.
 */
public final class RegionalOreRegistries {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Pipican.MOD_ID);
    public static final Map<String,DeferredBlock<Block>> NODES;
    public static final Map<String,DeferredItem<Item>> MATERIALS;
    /** Boss catalysts are earned from cleared regional trial arenas. */
    public static final Map<String,DeferredItem<Item>> BOSS_CORES;
    static {
        Map<String,DeferredBlock<Block>> blocks=new LinkedHashMap<>();
        Map<String,DeferredItem<Item>> items=new LinkedHashMap<>();
        for(var ore:RegionalOreCatalog.ORES){
            blocks.put(ore.id(),com.pipican.ore.PipicanOres.block(ore.mineOre()+"_ore"));
            items.put(ore.id(),com.pipican.ore.PipicanOres.material(ore.id()));
        }
        Map<String,DeferredItem<Item>> cores=new LinkedHashMap<>();
        for(var ore:RegionalOreCatalog.ORES)
            cores.put(ore.region(),ITEMS.registerSimpleItem(ore.region()+"_boss_core"));
        BOSS_CORES=Map.copyOf(cores);
        NODES=Map.copyOf(blocks);
        MATERIALS=Map.copyOf(items);
    }
    private RegionalOreRegistries(){}
    /** Called once on the mod event bus during the mod constructor. */
    public static void register(IEventBus bus){
        ITEMS.register(bus);
    }
}

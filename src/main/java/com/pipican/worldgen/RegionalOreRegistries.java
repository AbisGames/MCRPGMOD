package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registered 27 regional mining nodes and 27 corresponding raw materials.
 * The block has no automatic overworld ore generation feature: mining sites
 * will expose it only from renewable nodes inside protected structures.
 */
public final class RegionalOreRegistries {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Pipican.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Pipican.MOD_ID);
    public static final Map<String,DeferredBlock<Block>> NODES;
    public static final Map<String,DeferredItem<Item>> MATERIALS;
    /** Boss catalysts are earned from cleared regional trial arenas. */
    public static final Map<String,DeferredItem<Item>> BOSS_CORES;
    static {
        Map<String,DeferredBlock<Block>> blocks=new LinkedHashMap<>();
        Map<String,DeferredItem<Item>> items=new LinkedHashMap<>();
        for(String id:List.of("frostcrystal","glacierite","wintersteel","stormsilver","thunderbasalt","skyiron","tideglass","mooncrystal","royal_silver","runic_iron","jade_essence","mirestone","pearlsilver","cinderite","lotus_opal","sunglass","sunstone","saffron_gold","verdantite","tidalite","dawnbronze","redrock_iron","skycrystal","lifebloom_crystal","umbrite","bloodstone","abyssal_shard")){
            DeferredBlock<Block> node=BLOCKS.registerSimpleBlock(id+"_ore",
                props->props.strength(4.5f,12.0f).requiresCorrectToolForDrops());
            blocks.put(id,node);
            items.put(id,ITEMS.registerSimpleItem(id));
        }
        Map<String,DeferredItem<Item>> cores=new LinkedHashMap<>();
        for(String region:List.of("whitecrown_icecap","northern_ice_sea","pinewall_taiga","fjordmark","stormcoast","khagan_steppe","western_ocean","elderwood","heartlands","greyspine_mountains","jade_empire","marshfen","inner_sea","ashlands","lotus_delta","sunscorch_desert","riverlands","spice_coast","emerald_jungle","coral_isles","goldgrass_savanna","redrock_badlands","aetheris_expanse","verdant_depths","umbral_chasm","crimson_wilds","shimmering_reefs"))
            cores.put(region,ITEMS.registerSimpleItem(region+"_boss_core"));
        BOSS_CORES=Map.copyOf(cores);
        NODES=Map.copyOf(blocks);
        MATERIALS=Map.copyOf(items);
    }
    private RegionalOreRegistries(){}
    /** Called once on the mod event bus during the mod constructor. */
    public static void register(IEventBus bus){
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}

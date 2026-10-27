package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Original decorative terrain palette for each of the 22 realms and 5 rare biomes.
 * Resource generation is deliberately separate from ores, dungeons and buildings.
 * These blocks can also be reused by future castle facades and village structures.
 */
public final class RegionalNatureBlocks {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Pipican.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Pipican.MOD_ID);
    public record Palette(DeferredBlock<? extends Block> strata,
                          DeferredBlock<? extends Block> trunk,
                          DeferredBlock<? extends Block> canopy,
                          DeferredBlock<? extends Block> bloom){}
    public static final Map<String,Palette> PALETTES;
    static {
        LinkedHashMap<String,Palette> all=new LinkedHashMap<>();
        LinkedHashSet<String> ids=new LinkedHashSet<>();
        for(WorldLayout.Region region:WorldLayout.REGIONS)ids.add(region.id());
        for(WorldLayout.ExtraBiome biome:WorldLayout.EXTRA_BIOMES)ids.add(biome.id());
        for(String id:ids) {
            var strata=BLOCKS.registerSimpleBlock(id+"_strata",
                p->p.strength(3.3F,9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
            var trunk=BLOCKS.registerBlock(id+"_trunk",RotatedPillarBlock::new,
                p->p.strength(2.1F).sound(SoundType.WOOD));
            var canopy=BLOCKS.registerBlock(id+"_canopy",properties -> new TintedParticleLeavesBlock(0.02F,properties),
                p->p.strength(0.2F).randomTicks().noOcclusion().sound(SoundType.GRASS));
            var bloom=BLOCKS.registerBlock(id+"_bloom",TallGrassBlock::new,
                p->p.noCollision().instabreak().noOcclusion().sound(SoundType.GRASS));
            ITEMS.registerSimpleBlockItem(strata);
            ITEMS.registerSimpleBlockItem(trunk);
            ITEMS.registerSimpleBlockItem(canopy);
            ITEMS.registerSimpleBlockItem(bloom);
            all.put(id,new Palette(strata,trunk,canopy,bloom));
        }
        PALETTES=Map.copyOf(all);
    }
    private RegionalNatureBlocks(){}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
}

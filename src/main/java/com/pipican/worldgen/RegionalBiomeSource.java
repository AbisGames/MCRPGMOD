package com.pipican.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Worldgen biome source for the scaled authored world.
 *
 * <p>Each quart-coordinate is mapped to the original 22 macro regions and
 * 120 jittered Voronoi subregions. Five exceptional fantasy biomes occupy
 * suitable parts of their parent territories, never replacing capital plots.
 * The source is deterministic and safe when chunks are requested out of order.
 *
 * <p>The biome source does not itself sculpt terrain or place structures.
 */
public final class RegionalBiomeSource extends BiomeSource {
    public static final MapCodec<RegionalBiomeSource> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(source -> source.biomes),
            Codec.LONG.optionalFieldOf("seed",0L).forGetter(source -> source.seed)
        ).apply(instance, RegionalBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final long seed;
    private final Map<String,Holder<Biome>> byId;
    private final Holder<Biome> fallback;
    private final SubregionSampler sampler;

    private RegionalBiomeSource(HolderSet<Biome> biomes,long seed){
        this.biomes=biomes;
        this.seed=seed;
        this.sampler=new SubregionSampler();
        Map<String,Holder<Biome>> keyed=new HashMap<>();
        for(Holder<Biome> holder:biomes)
            holder.unwrapKey().ifPresent(key -> keyed.put(key.identifier().toString(),holder));
        if(keyed.isEmpty())throw new IllegalArgumentException("RegionalBiomeSource requires biomes");
        this.byId=Map.copyOf(keyed);
        this.fallback=keyed.getOrDefault("minecraft:plains",biomes.stream().findFirst().orElseThrow());
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec(){return CODEC;}

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes(){return biomes.stream();}

    @Override
    public Holder<Biome> getNoiseBiome(int quartX,int quartY,int quartZ,Climate.Sampler climate){
        int x=quartX*4,z=quartZ*4,y=quartY*4;
        Optional<SubregionSampler.Subregion> sub=sampler.at(seed,x,z);
        if(sub.isEmpty())return fallback;
        SubregionSampler.Subregion region=sub.get();
        String path=region.parent()+"/"+region.id();

        // Large-scale seed-driven exceptional terrain patches.
        double detail=WorldLayout.fractal(seed^0x6A09E667F3BCC909L,x,z,2600);
        if(region.parent().equals("greyspine_mountains")&&y>250&&detail>.38)
            path="aetheris_expanse";
        else if(region.parent().equals("greyspine_mountains")&&y<0&&detail>.3)
            path="umbral_chasm";
        else if(region.parent().equals("emerald_jungle")&&detail>.28)
            path="verdant_depths";
        else if(region.parent().equals("ashlands")&&detail>.30)
            path="crimson_wilds";
        else if(region.parent().equals("coral_isles")&&y<90&&detail>.30)
            path="shimmering_reefs";

        return byId.getOrDefault("pipican:"+path,fallback);
    }
}

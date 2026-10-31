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
    private final List<ProtectedSite> protectedSites;

    /** Native jigsaw generators only see registered biome eligibility; these
     * protected region variants are intentionally absent from structure tags.
     */
    private record ProtectedSite(int x,int z,int radius,String biomeId) {}
    private static List<ProtectedSite> readProtectedSites() {
        List<ProtectedSite> sites=new ArrayList<>();
        for(var zone:CapitalStructureExclusion.all()) {
            String region=WorldLayout.authoredRegionAt(zone.x(),zone.z())
                .orElseThrow().id();
            sites.add(new ProtectedSite(zone.x(),zone.z(),zone.radius()+180,
                "pipican:protected/"+region));
        }
        for(var landmark:new LandmarkCatalog().all()) {
            if(landmark.tier().equals("capital"))continue;
            sites.add(new ProtectedSite(landmark.x(),landmark.z(),
                Math.max(landmark.radius()+150,160),
                "pipican:protected/"+landmark.region()));
        }
        return List.copyOf(sites);
    }

    private RegionalBiomeSource(HolderSet<Biome> biomes,long seed){
        this.biomes=biomes;
        this.seed=seed;
        this.sampler=new SubregionSampler();
        this.protectedSites=readProtectedSites();
        Map<String,Holder<Biome>> keyed=new HashMap<>();
        for(Holder<Biome> holder:biomes)
            holder.unwrapKey().ifPresent(key -> keyed.put(key.identifier().toString(),holder));
        if(keyed.isEmpty())throw new IllegalArgumentException("RegionalBiomeSource requires biomes");
        this.byId=Map.copyOf(keyed);
        // Never fall back to a vanilla biome inside the bespoke RPG preset:
        // vanilla structure biome tags must not gain an accidental foothold.
        this.fallback=keyed.getOrDefault("pipican:heartlands/farmland_plains",
            biomes.stream().findFirst().orElseThrow());
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec(){return CODEC;}

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes(){return biomes.stream();}

    @Override
    public Holder<Biome> getNoiseBiome(int quartX,int quartY,int quartZ,Climate.Sampler climate){
        int x=quartX*4,z=quartZ*4,y=quartY*4;
        // A dedicated sky biome retains Aetheris weather and visuals but
        // is not eligible for native jigsaws on our seven authored islands.
        if(SkyIslandLayout.themedSkyAt(x,y,z))
            return byId.getOrDefault("pipican:aetheris_sanctuary",fallback);
        // Dedicated safety biomes reserve the two unique land/underground
        // landmarks from vanilla and native RPG structures alike.
        long bx=(long)x-3500,bz=(long)z-21300;
        if(bx*bx+bz*bz<260L*260L)
            return byId.getOrDefault("pipican:redrock_sanctuary",fallback);
        long ux=(long)x-4900,uz=(long)z;
        if(ux*ux+uz*uz<240L*240L)
            return byId.getOrDefault(y<45?"pipican:umbral_sanctuary":
                "pipican:protected/greyspine_mountains",fallback);
        // Reserve all 70 authored sites during *native* chunk generation:
        // structure sets never target these biome variants.
        for(ProtectedSite site:protectedSites) {
            long dx=(long)x-site.x(),dz=(long)z-site.z(),radius=site.radius();
            if(dx*dx+dz*dz<=radius*radius)
                return byId.getOrDefault(site.biomeId(),fallback);
        }
        // A 320-block border buffer also avoids jigsaw collisions between
        // adjacent regional grids; regional sanctuary biomes keep their theme.
        // It is measured along the *warped* border (the same one the
        // subregion sampler uses), so region edges do not come out straight.
        var warped=WorldLayout.warpedAt(seed,x,z);
        if(warped.isPresent()&&warped.get().borderDistance()<320)
            return byId.getOrDefault("pipican:protected/"+warped.get().region().id(),fallback);
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
        else if(region.parent().equals("emerald_jungle")&&y<44&&detail>.28)
            path="verdant_depths";
        else if(region.parent().equals("ashlands")&&detail>.30)
            path="crimson_wilds";
        else if(region.parent().equals("coral_isles")&&y<90&&detail>.30)
            path="shimmering_reefs";

        return byId.getOrDefault("pipican:"+path,fallback);
    }
}

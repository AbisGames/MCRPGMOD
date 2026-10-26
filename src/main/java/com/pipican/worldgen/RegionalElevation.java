package com.pipican.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Additional region-aware topographic density for the 50k RPG overworld.
 *
 * <p>Unlike the generic vanilla density router, mountains are encouraged in
 * Greyspine, Whitecrown and Jade, while oceans remain sunken. The contribution
 * is tapered near the surface and approaches zero below Y=32, allowing vanilla
 * cave networks and normal ore distribution to remain largely intact.
 *
 * <p>Distances, ridges and their coordinate warp are deterministic and never
 * assume a fixed terrain height for castle construction.
 */
public record RegionalElevation(double strength) implements DensityFunction.SimpleFunction {
    public static final MapCodec<RegionalElevation> MAP_CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("strength",1.0).forGetter(RegionalElevation::strength)
        ).apply(instance,RegionalElevation::new));

    private static final KeyDispatchDataCodec<RegionalElevation> CODEC =
        KeyDispatchDataCodec.of(MAP_CODEC);

    @Override public double compute(DensityFunction.FunctionContext context){
        int x=context.blockX(),y=context.blockY(),z=context.blockZ();
        if(y<=32 || y>430)return 0.0;
        var reg=WorldLayout.regionAt(0L,x,z);
        if(reg.isEmpty())return 0.0;
        var r=reg.get();
        double target=switch(r.id()){
            case "greyspine_mountains" -> 210.0;
            case "jade_empire","whitecrown_icecap" -> 135.0;
            case "fjordmark","stormcoast","aetheris_expanse" -> 95.0;
            case "ashlands","redrock_badlands" -> 75.0;
            case "pinewall_taiga","elderwood","emerald_jungle" -> 50.0;
            case "khagan_steppe","goldgrass_savanna" -> 15.0;
            case "marshfen","lotus_delta","heartlands","riverlands" -> 8.0;
            case "western_ocean","northern_ice_sea","inner_sea","coral_isles" -> -35.0;
            default -> 28.0;
        };

        // Avoid perfectly sharp macro borders: density contribution smoothly
        // attenuates over the last 500 blocks of a rectangular territory.
        double edge=Math.max(0,Math.min(Math.min(x-r.minX(),r.maxX()-x),
            Math.min(z-r.minZ(),r.maxZ()-z)));
        double t=Math.min(1.0,edge/500.0);
        double blend=t*t*(3-2*t);

        // Eroded ridges and extensive valleys depend on two noise octaves.
        double roughness=WorldLayout.fractal(5821L,x,z,1250);
        double ridge=0.82+0.26*roughness;
        double upper=98.0+Math.max(0,target)*0.95;
        double vertical=Math.max(0,Math.min(1,(upper-y)/110.0));
        double nearGround=Math.min(1,Math.max(0,(y-32)/25.0));
        return strength*(target/130.0)*ridge*vertical*nearGround*blend;
    }
    @Override public double minValue(){return -4*strength;}
    @Override public double maxValue(){return 4*strength;}
    @Override public DensityFunction mapAll(DensityFunction.Visitor visitor){
        return visitor.apply(this);
    }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){
        return CODEC;
    }
}

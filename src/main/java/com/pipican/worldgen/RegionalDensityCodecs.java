package com.pipican.worldgen;

import com.pipican.Pipican;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers RPG terrain density codecs before world presets are parsed. */
public final class RegionalDensityCodecs {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
        DeferredRegister.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE,Pipican.MOD_ID);
    static {
        TYPES.register("regional_elevation",()->RegionalElevation.MAP_CODEC);
    }
    private RegionalDensityCodecs(){}
    public static void register(IEventBus bus){TYPES.register(bus);}
}

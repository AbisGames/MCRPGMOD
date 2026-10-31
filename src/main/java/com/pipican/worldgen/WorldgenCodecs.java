package com.pipican.worldgen;

import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the regional biome source during NeoForge's registration phase.
 * Never write to the already-frozen vanilla registry in the mod constructor.
 */
public final class WorldgenCodecs {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> SOURCES =
        DeferredRegister.create(BuiltInRegistries.BIOME_SOURCE,Pipican.MOD_ID);
    static {
        SOURCES.register("regional",()->RegionalBiomeSource.CODEC);
    }
    private WorldgenCodecs(){}
    public static void register(IEventBus bus) {
        SOURCES.register(bus);
    }
}

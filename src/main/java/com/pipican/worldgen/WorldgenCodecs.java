package com.pipican.worldgen;

import com.pipican.Pipican;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Registers the custom biome source codec before datapack world presets load. */
public final class WorldgenCodecs {
    private static boolean registered;
    private WorldgenCodecs(){}
    public static synchronized void register(){
        if(registered)return;
        Registry.register(BuiltInRegistries.BIOME_SOURCE,
            Pipican.id("regional"),RegionalBiomeSource.CODEC);
        registered=true;
    }
}

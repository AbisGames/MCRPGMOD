package com.pipican.worldgen;

import com.pipican.Pipican;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Applies the chosen world size only to saves created with the RPG preset.
 * Vanilla and other mods' world presets retain their original world borders.
 */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class RegionalWorldBorder {
    private RegionalWorldBorder(){}
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        ServerLevel overworld=event.getServer().overworld();
        if(overworld.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource) {
            overworld.getWorldBorder().setCenter(0,0);
            overworld.getWorldBorder().setSize(2.0*WorldLayout.HALF_SIZE);
            Pipican.LOGGER.info("Regional RPG world border set to {} blocks",WorldLayout.HALF_SIZE*2);
        }
    }
}

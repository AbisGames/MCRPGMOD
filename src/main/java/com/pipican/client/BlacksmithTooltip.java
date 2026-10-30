package com.pipican.client;

import com.pipican.Pipican;
import com.pipican.blacksmith.BlacksmithUpgrades;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Metadata display only; the NPC interface will call the server backend later. */
@EventBusSubscriber(modid=Pipican.MOD_ID,value=Dist.CLIENT)
public final class BlacksmithTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        try {
            var state=BlacksmithUpgrades.State.read(event.getItemStack());
            if(!state.region().isEmpty()) event.getToolTip().add(Component.translatable("blacksmith.pipican.upgrade",
                Component.translatable("blacksmith.pipican.tier."+state.tier().name().toLowerCase(java.util.Locale.ROOT)),state.level(),state.region()));
        }catch(IllegalArgumentException invalid){ /* Never let damaged custom data break the item tooltip. */ }
    }
}

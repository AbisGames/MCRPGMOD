package com.pipican.resource;

import com.pipican.Pipican;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** The server rejects the break, and tool-conditioned loot also rejects explosions/other paths. */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class RareMiningRules {
    private RareMiningRules(){}
    public static boolean canMine(RareResources.Deposit deposit,ItemStack tool){
        return !tool.isEmpty() && BuiltInRegistries.ITEM.getKey(tool.getItem()).toString().equals(deposit.tool());
    }
    @SubscribeEvent public static void breaking(BreakBlockEvent event){
        if(event.getPlayer().isCreative()) return;
        for(var deposit:RareResources.DEPOSITS) if(event.getState().is(RareResources.ORES.get(deposit.id()).get())){
            if(!canMine(deposit,event.getPlayer().getMainHandItem())) event.setCanceled(true);
            return;
        }
    }
}

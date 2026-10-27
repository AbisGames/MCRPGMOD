package com.pipican.worldgen;

import com.pipican.Pipican;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Gives builders a dedicated creative inventory for all original terrain
 * blocks, trunks, canopies and undergrowth, sorted by their fantasy region.
 */
public final class RegionalNatureTab {
    public static final DeferredRegister<CreativeModeTab> TABS=
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Pipican.MOD_ID);
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB =
        TABS.register("regional_nature",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pipican.regional_nature"))
            .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
            .icon(()->RegionalNatureBlocks.PALETTES.get("elderwood").strata().get()
                .asItem().getDefaultInstance())
            .displayItems((params,output)->{
                for(var region:WorldLayout.REGIONS){
                    var p=RegionalNatureBlocks.PALETTES.get(region.id());
                    output.accept(p.strata().get());
                    output.accept(p.ground().get());
                    output.accept(p.trunk().get());
                    output.accept(p.canopy().get());
                    output.accept(p.bloom().get());
                }
                for(var region:WorldLayout.EXTRA_BIOMES){
                    var p=RegionalNatureBlocks.PALETTES.get(region.id());
                    output.accept(p.strata().get());
                    output.accept(p.ground().get());
                    output.accept(p.trunk().get());
                    output.accept(p.canopy().get());
                    output.accept(p.bloom().get());
                }
            }).build());
    private RegionalNatureTab(){}
    public static void register(IEventBus bus){TABS.register(bus);}
}

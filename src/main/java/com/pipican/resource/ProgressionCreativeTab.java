package com.pipican.resource;

import com.pipican.Pipican;
import com.pipican.boss.BossContent;
import com.pipican.worldgen.RegionalOreRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProgressionCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Pipican.MOD_ID);
    static {
        TABS.register("progression",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.pipican.progression"))
            .icon(()->BossContent.MORVATH_SIGIL.get().getDefaultInstance()).displayItems((parameters,output)->{
                output.accept(BossContent.ALTAR.get()); output.accept(BossContent.CORE.get());
                output.accept(BossContent.MORVATH_SIGIL.get()); output.accept(BossContent.HEART.get());
                output.accept(BossContent.FROSTBOUND_SIGIL.get()); output.accept(BossContent.FROSTBOUND_TROPHY.get());
                output.accept(BossContent.SKOLLVAR_SIGIL.get()); output.accept(BossContent.SKOLLVAR_TROPHY.get());
                output.accept(BossContent.ELDAROTH_SIGIL.get()); output.accept(BossContent.ELDAROTH_TROPHY.get());
                output.accept(BossContent.FALLEN_KING_SIGIL.get()); output.accept(BossContent.FALLEN_KING_TROPHY.get());
                output.accept(BossContent.KRAGMOR_SIGIL.get()); output.accept(BossContent.KRAGMOR_TROPHY.get());
                output.accept(BossContent.NOCTHAR_SIGIL.get()); output.accept(BossContent.NOCTHAR_TROPHY.get());
                RareResources.MATERIALS.values().forEach(item->output.accept(item.get()));
                RareResources.ORES.values().forEach(block->output.accept(block.get()));
                RareResources.PICKAXES.values().forEach(item->output.accept(item.get()));
                RegionalOreRegistries.MATERIALS.values().forEach(item->output.accept(item.get()));
                RegionalOreRegistries.BOSS_CORES.values().forEach(item->output.accept(item.get()));
            }).build());
    }
    private ProgressionCreativeTab(){}
    public static void register(IEventBus bus){ TABS.register(bus); }
}

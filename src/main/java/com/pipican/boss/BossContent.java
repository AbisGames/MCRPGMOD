package com.pipican.boss;

import com.pipican.Pipican;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Functional Morvath content only; unimplemented bosses receive no placeholder summons. */
public final class BossContent {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Pipican.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Pipican.MOD_ID);
    public static final DeferredBlock<RitualAltarBlock> ALTAR=BLOCKS.registerBlock("ritual_altar",RitualAltarBlock::new,p->p.strength(5,1200).lightLevel(s->7));
    public static final DeferredItem<Item> CORE=ITEMS.registerSimpleItem("ritual_core");
    public static final DeferredItem<Item> MORVATH_SIGIL=ITEMS.registerSimpleItem("morvath_sigil");
    public static final DeferredItem<Item> FROSTBOUND_SIGIL=ITEMS.registerSimpleItem("frostbound_colossus_sigil");
    public static final DeferredItem<Item> FROSTBOUND_TROPHY=ITEMS.registerSimpleItem("frostbound_colossus_trophy");
    public static final DeferredItem<Item> SKOLLVAR_SIGIL=ITEMS.registerSimpleItem("skollvar_winterfang_sigil");
    public static final DeferredItem<Item> SKOLLVAR_TROPHY=ITEMS.registerSimpleItem("skollvar_winterfang_trophy");
    public static final DeferredItem<Item> ELDAROTH_SIGIL=ITEMS.registerSimpleItem("eldaroth_treant_sigil");
    public static final DeferredItem<Item> ELDAROTH_TROPHY=ITEMS.registerSimpleItem("eldaroth_treant_trophy");
    public static final DeferredItem<Item> FALLEN_KING_SIGIL=ITEMS.registerSimpleItem("fallen_king_sigil");
    public static final DeferredItem<Item> FALLEN_KING_TROPHY=ITEMS.registerSimpleItem("fallen_king_trophy");
    public static final DeferredItem<Item> KRAGMOR_SIGIL=ITEMS.registerSimpleItem("kragmor_canyon_warlord_sigil");
    public static final DeferredItem<Item> KRAGMOR_TROPHY=ITEMS.registerSimpleItem("kragmor_canyon_warlord_trophy");
    public static final DeferredItem<Item> NOCTHAR_SIGIL=ITEMS.registerSimpleItem("nocthar_void_sovereign_sigil");
    public static final DeferredItem<Item> NOCTHAR_TROPHY=ITEMS.registerSimpleItem("nocthar_void_sovereign_trophy");
    public static final DeferredItem<Item> AETHERION_SIGIL=ITEMS.registerSimpleItem("aetherion_sky_guardian_sigil");
    public static final DeferredItem<Item> AETHERION_TROPHY=ITEMS.registerSimpleItem("aetherion_sky_guardian_trophy");
    public static final DeferredItem<Item> VERMILLION_SIGIL=ITEMS.registerSimpleItem("vermillion_blood_monarch_sigil");
    public static final DeferredItem<Item> VERMILLION_TROPHY=ITEMS.registerSimpleItem("vermillion_blood_monarch_trophy");
    public static final DeferredItem<Item> LONGWEI_SIGIL=ITEMS.registerSimpleItem("longwei_jade_dragon_sigil");
    public static final DeferredItem<Item> LONGWEI_TROPHY=ITEMS.registerSimpleItem("longwei_jade_dragon_trophy");
    public static final DeferredItem<Item> MORGATHA_SIGIL=ITEMS.registerSimpleItem("morgatha_swamp_queen_sigil");
    public static final DeferredItem<Item> MORGATHA_TROPHY=ITEMS.registerSimpleItem("morgatha_swamp_queen_trophy");
    public static final DeferredItem<Item> AURELIOS_SIGIL=ITEMS.registerSimpleItem("aurelios_sunken_emperor_sigil");
    public static final DeferredItem<Item> AURELIOS_TROPHY=ITEMS.registerSimpleItem("aurelios_sunken_emperor_trophy");
    public static final DeferredItem<Item> LIANHUA_SIGIL=ITEMS.registerSimpleItem("lianhua_lotus_serpent_sigil");
    public static final DeferredItem<Item> LIANHUA_TROPHY=ITEMS.registerSimpleItem("lianhua_lotus_serpent_trophy");
    public static final DeferredItem<Item> AKHMAR_SIGIL=ITEMS.registerSimpleItem("akhmar_sand_emperor_sigil");
    public static final DeferredItem<Item> AKHMAR_TROPHY=ITEMS.registerSimpleItem("akhmar_sand_emperor_trophy");
    public static final DeferredItem<Item> NETHARIS_SIGIL=ITEMS.registerSimpleItem("netharis_river_guardian_sigil");
    public static final DeferredItem<Item> NETHARIS_TROPHY=ITEMS.registerSimpleItem("netharis_river_guardian_trophy");
    public static final DeferredItem<Item> ZAHIRA_SIGIL=ITEMS.registerSimpleItem("zahira_crimson_corsair_sigil");
    public static final DeferredItem<Item> ZAHIRA_TROPHY=ITEMS.registerSimpleItem("zahira_crimson_corsair_trophy");
    public static final DeferredItem<Item> XALTEK_SIGIL=ITEMS.registerSimpleItem("xaltek_emerald_devourer_sigil");
    public static final DeferredItem<Item> XALTEK_TROPHY=ITEMS.registerSimpleItem("xaltek_emerald_devourer_trophy");
    public static final DeferredItem<Item> CORALLITH_SIGIL=ITEMS.registerSimpleItem("corallith_reef_colossus_sigil");
    public static final DeferredItem<Item> CORALLITH_TROPHY=ITEMS.registerSimpleItem("corallith_reef_colossus_trophy");
    public static final DeferredItem<Item> AZHAR_SIGIL=ITEMS.registerSimpleItem("azhar_golden_behemoth_sigil");
    public static final DeferredItem<Item> AZHAR_TROPHY=ITEMS.registerSimpleItem("azhar_golden_behemoth_trophy");
    public static final DeferredItem<Item> NHALKORR_SIGIL=ITEMS.registerSimpleItem("nhalkorr_leviathan_sigil");
    public static final DeferredItem<Item> NHALKORR_TROPHY=ITEMS.registerSimpleItem("nhalkorr_leviathan_trophy");
    public static final DeferredItem<Item> HROTHGAR_SIGIL=ITEMS.registerSimpleItem("hrothgar_drowned_jarl_sigil");
    public static final DeferredItem<Item> HROTHGAR_TROPHY=ITEMS.registerSimpleItem("hrothgar_drowned_jarl_trophy");
    public static final DeferredItem<Item> VALTHOR_SIGIL=ITEMS.registerSimpleItem("valthor_stormbringer_sigil");
    public static final DeferredItem<Item> VALTHOR_TROPHY=ITEMS.registerSimpleItem("valthor_stormbringer_trophy");
    public static final DeferredItem<Item> TENGRI_SIGIL=ITEMS.registerSimpleItem("tengri_champion_sigil");
    public static final DeferredItem<Item> TENGRI_TROPHY=ITEMS.registerSimpleItem("tengri_champion_trophy");
    public static final DeferredItem<Item> THALASSOR_SIGIL=ITEMS.registerSimpleItem("thalassor_deep_tyrant_sigil");
    public static final DeferredItem<Item> THALASSOR_TROPHY=ITEMS.registerSimpleItem("thalassor_deep_tyrant_trophy");
    public static final DeferredItem<Item> HEART=ITEMS.registerSimpleItem("ashen_kings_heart",p->p.fireResistant());
    public static final DeferredItem<Item> INFERNIUM=ITEMS.registerSimpleItem("infernium",p->p.fireResistant());
    static { ITEMS.registerSimpleBlockItem(ALTAR); }
    private BossContent(){}
    public static void register(IEventBus bus){ BLOCKS.register(bus); ITEMS.register(bus); }
}

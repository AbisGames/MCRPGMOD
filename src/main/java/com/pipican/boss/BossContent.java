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
    public static final DeferredItem<Item> HEART=ITEMS.registerSimpleItem("ashen_kings_heart",p->p.fireResistant());
    public static final DeferredItem<Item> INFERNIUM=ITEMS.registerSimpleItem("infernium",p->p.fireResistant());
    static { ITEMS.registerSimpleBlockItem(ALTAR); }
    private BossContent(){}
    public static void register(IEventBus bus){ BLOCKS.register(bus); ITEMS.register(bus); }
}

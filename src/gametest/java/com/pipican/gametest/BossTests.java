package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.boss.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Real registry/entity serialization tests plus deterministic combat timing regressions. */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class BossTests {
    private static void refreshMockPositions(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.player.Player... players){
        for(var player:players)player.teleportTo(level,player.getX(),player.getY(),player.getZ(),java.util.Set.of(),player.getYRot(),player.getXRot(),false);
    }
    private static final Map<String,Consumer<GameTestHelper>> TESTS=Map.ofEntries(
        Map.entry("boss_combat_reload",BossTests::combatReload),
        Map.entry("boss_phase_latch",BossTests::phaseLatch),
        Map.entry("boss_entity_roundtrip",BossTests::entityRoundtrip),
        Map.entry("boss_encounter_reservations",BossTests::encounterReservations),
        Map.entry("boss_summoning_lifecycle",BossTests::summoningLifecycle),
        Map.entry("boss_unload_and_cleanup",BossTests::unloadAndCleanup),
        Map.entry("morvath_real_attacks",BossTests::realAttacks),
        Map.entry("ritual_recipes",BossTests::ritualRecipes),
        Map.entry("ritual_interaction_and_loot",BossTests::ritualInteraction),
        Map.entry("boss_design_catalog",BossTests::designCatalog),
        Map.entry("nocthar_deep_encounter",BossTests::noctharDeepEncounter),
        Map.entry("aetherion_high_encounter",BossTests::aetherionHighEncounter),
        Map.entry("vermillion_crimson_encounter",BossTests::vermillionCrimsonEncounter),
        Map.entry("longwei_jade_encounter",BossTests::longweiJadeEncounter),
        Map.entry("morgatha_marshfen_encounter",BossTests::morgathaMarshfenEncounter),
        Map.entry("aurelios_inner_sea_encounter",BossTests::aureliosInnerSeaEncounter),
        Map.entry("lianhua_lotus_encounter",BossTests::lianhuaLotusEncounter),
        Map.entry("akhmar_sun_encounter",BossTests::akhmarSunEncounter),
        Map.entry("akhmar_combat",BossTests::akhmarCombat),
        Map.entry("netharis_river_encounter",BossTests::netharisRiverEncounter),
        Map.entry("netharis_combat",BossTests::netharisCombat),
        Map.entry("zahira_spice_encounter",BossTests::zahiraSpiceEncounter),
        Map.entry("zahira_combat",BossTests::zahiraCombat),
        Map.entry("xaltek_combat",BossTests::xaltekCombat),
        Map.entry("xaltek_encounter",BossTests::xaltekEncounter),
        Map.entry("corallith_combat",BossTests::corallithCombat),
        Map.entry("corallith_encounter",BossTests::corallithEncounter),
        Map.entry("azhar_combat",BossTests::azharCombat),
        Map.entry("azhar_encounter",BossTests::azharEncounter),
        Map.entry("nhalkorr_combat",BossTests::nhalkorrCombat),
        Map.entry("nhalkorr_encounter",BossTests::nhalkorrEncounter),
        Map.entry("hrothgar_combat",BossTests::hrothgarCombat),
        Map.entry("hrothgar_encounter",BossTests::hrothgarEncounter),
        Map.entry("valthor_combat",BossTests::valthorCombat),
        Map.entry("valthor_encounter",BossTests::valthorEncounter),
        Map.entry("tengri_combat",BossTests::tengriCombat),
        Map.entry("tengri_encounter",BossTests::tengriEncounter),
        Map.entry("thalassor_combat",BossTests::thalassorCombat),
        Map.entry("thalassor_encounter",BossTests::thalassorEncounter),
        Map.entry("durmagon_combat",BossTests::durmagonCombat),
        Map.entry("durmagon_encounter",BossTests::durmagonEncounter),
        Map.entry("sylthera_combat",BossTests::syltheraCombat),
        Map.entry("sylthera_encounter",BossTests::syltheraEncounter),
        Map.entry("lumynara_combat",BossTests::lumynaraCombat),
        Map.entry("lumynara_encounter",BossTests::lumynaraEncounter));
    @SubscribeEvent public static void functions(RegisterEvent event){
        event.register(Registries.TEST_FUNCTION,registry->TESTS.forEach((name,fn)->registry.register(Pipican.id(name),fn)));
    }
    @SubscribeEvent public static void tests(RegisterGameTestsEvent event){
        Holder<TestEnvironmentDefinition<?>> env=event.registerEnvironment(Pipican.id("boss_test"),new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name->event.registerTest(Pipican.id(name),new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION,Pipican.id(name)),
            new TestData<>(env,net.minecraft.resources.Identifier.withDefaultNamespace("empty"),100,0,true))));
    }
    private static void designCatalog(GameTestHelper helper){
        helper.assertTrue(BossDesigns.all().size()==27 && BossDesigns.all().stream().filter(BossDesigns.Design::functional).count()==27,"functional boss design count differs from implemented bindings");
        var mechanics=new HashSet<String>(); var regional=new HashSet<String>();
        for(var design:BossDesigns.all()){
            helper.assertTrue(regional.add(design.region()) && design.abilities().size()==3 && design.animations().size()==10,"design mapping incomplete: "+design.id());
            for(var ability:design.abilities()) helper.assertTrue(mechanics.add(ability.behavior()) && !ability.counterplay().isBlank(),"copied or unreadable planned boss mechanic");
            for(var ingredient:design.sigil().ingredients()) helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ingredient.item()),"invalid planned sigil ingredient "+ingredient.item());
            helper.assertTrue(design.sigil().ingredients().stream().mapToInt(BossDesigns.Ingredient::count).sum()==9,"sigil requires wrong ingredient count");
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(design.loot().regional()),"invalid regional boss reward");
            design.loot().rare().ifPresent(drop->helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(drop.item()) && drop.chance()>0 && drop.chance()<=1,"invalid rare reward"));
            if(!design.functional()) helper.assertTrue(design.abilities().stream().allMatch(ability->ability.implementation().equals("planned")) && design.loot().trophyItem()==null,"planned trophy/ability presented as implemented");
        }
        var morvath=BossDesigns.require(BossCatalog.MORVATH.id());
        for(var ability:morvath.abilities()) helper.assertTrue(BossCombatState.Ability.valueOf(ability.id()).windup==ability.windup(),"functional attack timing differs from metadata");
        helper.assertTrue(morvath.health()==600 && morvath.damage()==12 && morvath.loot().rare().orElseThrow().chance()==0.25,"Morvath runtime defaults changed");
        var frost=BossDesigns.require("frostbound_colossus");
        helper.assertTrue(frost.functional() && frost.sigil().registered() && frost.loot().trophyItem().equals("pipican:frostbound_colossus_trophy"),
            "Frostbound implemented design is missing registered sigil/reward metadata");
        var skoll=BossDesigns.require("skollvar_winterfang");
        helper.assertTrue(skoll.functional() && skoll.sigil().registered() && skoll.loot().trophyItem().equals("pipican:skollvar_winterfang_trophy"),
            "Skollvar implemented design is missing sigil/reward metadata");
        var eldaroth=BossDesigns.require("eldaroth_treant");
        helper.assertTrue(eldaroth.functional() && eldaroth.sigil().registered() && eldaroth.loot().trophyItem().equals("pipican:eldaroth_treant_trophy"),
            "Eldaroth implemented design is missing sigil/reward metadata");
        var fallen=BossDesigns.require("fallen_king");
        helper.assertTrue(fallen.functional() && fallen.sigil().registered() && fallen.loot().trophyItem().equals("pipican:fallen_king_trophy"),
            "Fallen King implemented design is missing sigil/reward metadata");
        var kragmor=BossDesigns.require("kragmor_canyon_warlord");
        helper.assertTrue(kragmor.functional() && kragmor.sigil().registered() && kragmor.loot().trophyItem().equals("pipican:kragmor_canyon_warlord_trophy"),
            "Kragmor implemented design is missing sigil/reward metadata");
        var nocthar=BossDesigns.require("nocthar_void_sovereign");
        helper.assertTrue(nocthar.functional() && nocthar.sigil().registered() && nocthar.sigil().maxY()==-1 && nocthar.loot().rare().orElseThrow().chance()==0.2,
            "Nocthar implemented design is missing deep-biome/rare-reward metadata");
        var aetherion=BossDesigns.require("aetherion_sky_guardian");
        helper.assertTrue(aetherion.functional() && aetherion.sigil().registered() && aetherion.sigil().minY()==251
            && aetherion.loot().trophyItem().equals("pipican:aetherion_sky_guardian_trophy") && aetherion.loot().rare().orElseThrow().chance()==0.25,
            "Aetherion flight/high-altitude/unique-loot metadata is incomplete");
        var vermillion=BossDesigns.require("vermillion_blood_monarch");
        helper.assertTrue(vermillion.functional() && vermillion.sigil().registered() && vermillion.loot().trophyItem().equals("pipican:vermillion_blood_monarch_trophy")
            && vermillion.loot().rare().orElseThrow().chance()==0.25,"Vermillion channel/loot metadata is incomplete");
        var longwei=BossDesigns.require("longwei_jade_dragon");
        helper.assertTrue(longwei.functional()&&longwei.sigil().registered()&&longwei.loot().trophyItem().equals("pipican:longwei_jade_dragon_trophy"),"Longwei jade dragon metadata is incomplete");
        var morgatha=BossDesigns.require("morgatha_swamp_queen");
        helper.assertTrue(morgatha.functional()&&morgatha.sigil().registered()&&morgatha.loot().trophyItem().equals("pipican:morgatha_swamp_queen_trophy"),"Morgatha swamp queen metadata is incomplete");
        var aurelios=BossDesigns.require("aurelios_sunken_emperor");
        helper.assertTrue(aurelios.functional()&&aurelios.sigil().registered()&&aurelios.loot().trophyItem().equals("pipican:aurelios_sunken_emperor_trophy"),"Aurelios sunken emperor metadata is incomplete");
        var lianhua=BossDesigns.require("lianhua_lotus_serpent");
        helper.assertTrue(lianhua.functional()&&lianhua.sigil().registered()&&lianhua.loot().trophyItem().equals("pipican:lianhua_lotus_serpent_trophy"),"Lianhua lotus serpent metadata is incomplete");
        var akhmar=BossDesigns.require("akhmar_sand_emperor");
        helper.assertTrue(akhmar.functional()&&akhmar.sigil().registered()&&akhmar.loot().trophyItem().equals("pipican:akhmar_sand_emperor_trophy"),"Akhmar sand emperor metadata is incomplete");
        var netharis=BossDesigns.require("netharis_river_guardian");
        helper.assertTrue(netharis.functional()&&netharis.sigil().registered()&&netharis.loot().trophyItem().equals("pipican:netharis_river_guardian_trophy"),"Netharis river guardian metadata is incomplete");
        var zahira=BossDesigns.require("zahira_crimson_corsair");
        helper.assertTrue(zahira.functional()&&zahira.sigil().registered()&&zahira.loot().trophyItem().equals("pipican:zahira_crimson_corsair_trophy"),"Zahira crimson corsair metadata is incomplete");
        var xaltek=BossDesigns.require("xaltek_emerald_devourer");
        helper.assertTrue(xaltek.functional()&&xaltek.sigil().registered()&&xaltek.loot().trophyItem().equals("pipican:xaltek_emerald_devourer_trophy"),"Xaltek emerald devourer metadata is incomplete");
        var corallith=BossDesigns.require("corallith_reef_colossus");
        helper.assertTrue(corallith.functional()&&corallith.sigil().registered()&&corallith.loot().trophyItem().equals("pipican:corallith_reef_colossus_trophy"),"Corallith reef colossus metadata is incomplete");
        var azhar=BossDesigns.require("azhar_golden_behemoth");
        helper.assertTrue(azhar.functional()&&azhar.sigil().registered()&&azhar.loot().trophyItem().equals("pipican:azhar_golden_behemoth_trophy"),"Azhar golden behemoth metadata is incomplete");
        var nhalkorr=BossDesigns.require("nhalkorr_leviathan");
        helper.assertTrue(nhalkorr.functional()&&nhalkorr.sigil().registered()&&nhalkorr.loot().trophyItem().equals("pipican:nhalkorr_leviathan_trophy"),"Nhalkorr leviathan metadata is incomplete");
        var hrothgar=BossDesigns.require("hrothgar_drowned_jarl");
        helper.assertTrue(hrothgar.functional()&&hrothgar.sigil().registered()&&hrothgar.loot().trophyItem().equals("pipican:hrothgar_drowned_jarl_trophy"),"Hrothgar drowned jarl metadata is incomplete");
        var valthor=BossDesigns.require("valthor_stormbringer");
        helper.assertTrue(valthor.functional()&&valthor.sigil().registered()&&valthor.loot().trophyItem().equals("pipican:valthor_stormbringer_trophy"),"Valthor stormbringer metadata is incomplete");
        var tengri=BossDesigns.require("tengri_champion");
        helper.assertTrue(tengri.functional()&&tengri.sigil().registered()&&tengri.loot().trophyItem().equals("pipican:tengri_champion_trophy"),"Tengri champion metadata is incomplete");
        var thalassor=BossDesigns.require("thalassor_deep_tyrant");
        helper.assertTrue(thalassor.functional()&&thalassor.sigil().registered()&&thalassor.loot().trophyItem().equals("pipican:thalassor_deep_tyrant_trophy"),"Thalassor metadata is incomplete");
        var durmagon=BossDesigns.require("durmagon_runic_titan");
        helper.assertTrue(durmagon.functional()&&durmagon.sigil().registered()&&durmagon.loot().trophyItem().equals("pipican:durmagon_runic_titan_trophy"),"Durmagon metadata is incomplete");
        var sylthera=BossDesigns.require("sylthera_primordial_bloom");
        helper.assertTrue(sylthera.functional()&&sylthera.sigil().registered()&&sylthera.loot().trophyItem().equals("pipican:sylthera_primordial_bloom_trophy"),"Sylthera metadata is incomplete");
        var lumynara=BossDesigns.require("lumynara_abyssal_oracle");
        helper.assertTrue(lumynara.functional()&&lumynara.sigil().registered()&&lumynara.loot().trophyItem().equals("pipican:lumynara_abyssal_oracle_trophy"),"Lumynara metadata is incomplete");
        helper.succeed();
    }
    private static net.minecraft.core.BlockPos rejectSpawnAt;
    @SubscribeEvent public static void rejectSpawn(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(rejectSpawnAt!=null && event.getEntity() instanceof RegionalBossEntity && event.getEntity().blockPosition().equals(rejectSpawnAt)) event.setCanceled(true);
    }
    private static void ritualRecipes(GameTestHelper helper){
        var level=helper.getLevel();
        var rows=List.of(
            List.of(net.minecraft.world.item.Items.OBSIDIAN,net.minecraft.world.item.Items.AMETHYST_SHARD,net.minecraft.world.item.Items.OBSIDIAN,net.minecraft.world.item.Items.CRYING_OBSIDIAN,net.minecraft.world.item.Items.ENDER_PEARL,net.minecraft.world.item.Items.CRYING_OBSIDIAN,net.minecraft.world.item.Items.OBSIDIAN,net.minecraft.world.item.Items.DIAMOND,net.minecraft.world.item.Items.OBSIDIAN),
            List.of(net.minecraft.world.item.Items.AMETHYST_SHARD,net.minecraft.world.item.Items.GOLD_INGOT,net.minecraft.world.item.Items.AMETHYST_SHARD,net.minecraft.world.item.Items.GOLD_INGOT,net.minecraft.world.item.Items.ENDER_PEARL,net.minecraft.world.item.Items.GOLD_INGOT,net.minecraft.world.item.Items.AMETHYST_SHARD,net.minecraft.world.item.Items.GOLD_INGOT,net.minecraft.world.item.Items.AMETHYST_SHARD),
            List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("ashlands").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("ashlands").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.MAGMA_CREAM));
        String[] ids={"ritual_altar","ritual_core","morvath_sigil","frostbound_colossus_sigil","eldaroth_treant_sigil","fallen_king_sigil","kragmor_canyon_warlord_sigil","nocthar_void_sovereign_sigil","aetherion_sky_guardian_sigil","vermillion_blood_monarch_sigil","longwei_jade_dragon_sigil","morgatha_swamp_queen_sigil","aurelios_sunken_emperor_sigil","lianhua_lotus_serpent_sigil","akhmar_sand_emperor_sigil","netharis_river_guardian_sigil","zahira_crimson_corsair_sigil","xaltek_emerald_devourer_sigil","corallith_reef_colossus_sigil","azhar_golden_behemoth_sigil","nhalkorr_leviathan_sigil","hrothgar_drowned_jarl_sigil","valthor_stormbringer_sigil","tengri_champion_sigil","thalassor_deep_tyrant_sigil","durmagon_runic_titan_sigil","sylthera_primordial_bloom_sigil","lumynara_abyssal_oracle_sigil"};
        rows=new ArrayList<>(rows);
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("whitecrown_icecap").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.BLUE_ICE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("elderwood").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.OAK_SAPLING));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("heartlands").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.GOLDEN_APPLE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("redrock_badlands").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.POINTED_DRIPSTONE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("umbral_chasm").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.ENDER_PEARL));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("aetheris_expanse").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.FEATHER));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("crimson_wilds").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.NETHER_WART));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("jade_empire").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),BossContent.CORE.get(),net.minecraft.world.item.Items.GREEN_DYE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("marshfen").get(),net.minecraft.world.item.Items.SLIME_BALL,
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("marshfen").get(),BossContent.CORE.get()));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("inner_sea").get(),net.minecraft.world.item.Items.GOLD_INGOT,
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("inner_sea").get(),BossContent.CORE.get()));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("lotus_delta").get(),net.minecraft.world.item.Items.LILY_PAD,
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("lotus_delta").get(),BossContent.CORE.get()));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("sunscorch_desert").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("sunscorch_desert").get(),net.minecraft.world.item.Items.RABBIT_FOOT));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("riverlands").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("riverlands").get(),net.minecraft.world.item.Items.GLOW_INK_SAC));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("spice_coast").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("spice_coast").get(),net.minecraft.world.item.Items.FIRE_CHARGE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("emerald_jungle").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("emerald_jungle").get(),net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("coral_isles").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("coral_isles").get(),net.minecraft.world.item.Items.TUBE_CORAL));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("goldgrass_savanna").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("goldgrass_savanna").get(),net.minecraft.world.item.Items.BLAZE_POWDER));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("northern_ice_sea").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("northern_ice_sea").get(),net.minecraft.world.item.Items.NAUTILUS_SHELL));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("fjordmark").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("fjordmark").get(),net.minecraft.world.item.Items.ROTTEN_FLESH));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("stormcoast").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("stormcoast").get(),net.minecraft.world.item.Items.LIGHTNING_ROD));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("khagan_steppe").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("khagan_steppe").get(),net.minecraft.world.item.Items.WIND_CHARGE));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("western_ocean").get(),BossContent.CORE.get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("western_ocean").get(),net.minecraft.world.item.Items.PRISMARINE_SHARD));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("greyspine_mountains").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get(),
            BossContent.CORE.get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("greyspine_mountains").get(),net.minecraft.world.item.Items.AMETHYST_SHARD));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("verdant_depths").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get(),
            BossContent.CORE.get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("verdant_depths").get(),net.minecraft.world.item.Items.MOSS_BLOCK));
        rows.add(List.of(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get(),
            com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("shimmering_reefs").get(),com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get(),
            BossContent.CORE.get(),com.pipican.worldgen.RegionalOreRegistries.BOSS_CORES.get("shimmering_reefs").get(),net.minecraft.world.item.Items.GLOWSTONE_DUST));
        for(int index=0;index<ids.length;index++){
            var holder=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Pipican.id(ids[index]))).orElseThrow();
            var recipe=(net.minecraft.world.item.crafting.ShapedRecipe)holder.value();
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,rows.get(index).stream().map(net.minecraft.world.item.ItemStack::new).toList());
            helper.assertTrue(recipe.matches(input,level),"ritual recipe does not accept its exact nine ingredients: "+ids[index]);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.assemble(input).getItem()).equals(Pipican.id(ids[index])),"wrong ritual crafting output");
        }
        helper.succeed();
    }
    private static long hearts(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos arena){
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8)).stream()
            .filter(item->item.getItem().is(BossContent.HEART.get())).mapToLong(item->item.getItem().getCount()).sum();
    }
    private static void ritualInteraction(GameTestHelper helper){
        var level=helper.getLevel(); var altar=helper.absolutePos(new net.minecraft.core.BlockPos(1024,30,1024)); var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8) for(int z=-32;z<=32;z+=8) level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel(); owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());
        var stack=new net.minecraft.world.item.ItemStack(BossContent.MORVATH_SIGIL.get(),2); owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try {
            var state=level.getBlockState(altar);
            helper.assertTrue(BossEncounterService.summon(owner,arena,"unimplemented_boss")==BossEncounterService.Result.UNSUPPORTED_BOSS
                && !BossEncounters.get(level).conflicts(arena) && stack.getCount()==2,"unimplemented boss created an encounter or consumed a sigil");
            var tooHigh=new net.minecraft.core.BlockPos(altar.getX(),level.getMaxY()-1,altar.getZ());
            owner.snapTo(tooHigh.getX(),tooHigh.getY(),tooHigh.getZ());
            helper.assertTrue(BossEncounterService.summonMorvath(owner,tooHigh)==BossEncounterService.Result.INVALID_HEIGHT && stack.getCount()==2,"world ceiling allowed an incomplete arena");
            owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());
            helper.assertTrue(state.useItemOn(stack,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL && stack.getCount()==2,"wrong-biome altar consumed a sigil");
            var ashlands=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("ashlands/ash_waste_plains")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),ashlands).left().isPresent(),"ritual biome fixture failed");
            rejectSpawnAt=arena;
            helper.assertTrue(state.useItemOn(stack,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL && stack.getCount()==2,"failed spawn consumed sigil");
            helper.assertTrue(!BossEncounters.get(level).conflicts(arena),"failed spawn leaked reservation"); rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(stack,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS && stack.getCount()==1,"successful interaction did not consume exactly one sigil");
            helper.assertTrue(state.useItemOn(stack,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL && stack.getCount()==1,"repeated interaction consumed a second sigil");
            var boss=level.getEntitiesOfClass(RegionalBossEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).getFirst();
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); boss.saveWithoutId(output);
            helper.assertTrue(hearts(level,arena)==0,"loot existed before defeat");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);
            helper.assertTrue(hearts(level,arena)==1,"valid defeat did not create exactly one heart");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"regional reward missing");
            long rare=drops.stream().filter(item->item.getItem().is(BossContent.INFERNIUM.get())).mapToInt(item->item.getItem().getCount()).sum();
            helper.assertTrue(rare==(Math.floorMod(boss.getUUID().hashCode(),4)==0 ? 1 : 0),"Infernium roll differed from encounter roll");
            helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new net.minecraft.world.phys.AABB(arena).inflate(8)).isEmpty(),"experience reward missing");
            var replay=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD); replay.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
            replay.hurtServer(level,replay.damageSources().genericKill(),10000);
            helper.assertTrue(hearts(level,arena)==1,"pre-death reload duplicated boss loot");
            helper.assertTrue(level.getBlockState(altar).is(BossContent.ALTAR.get()),"reusable altar destroyed");
            helper.assertTrue(state.useItemOn(stack,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS && stack.isEmpty(),"altar could not be reused after completion");
            owner.setShiftKeyDown(true); state.useWithoutItem(level,owner,hit);
            helper.assertTrue(!BossEncounters.get(level).conflicts(arena),"owner could not cancel through empty-hand altar interaction");
            var cancelled=level.getEntitiesOfClass(RegionalBossEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(e->e.isAlive()).findFirst().orElseThrow();
            cancelled.hurtServer(level,cancelled.damageSources().genericKill(),10000);
            helper.assertTrue(hearts(level,arena)==1,"administrative cancellation awarded loot");
            var ice=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("whitecrown_icecap/aurora_plateau")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),ice).left().isPresent(),"Frostbound biome fixture failed");
            owner.setShiftKeyDown(false); owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BossContent.FROSTBOUND_SIGIL.get()));
            helper.assertTrue(state.useItemOn(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS,
                "Frostbound altar summon failed");
            helper.assertTrue(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"successful Frostbound summon did not consume its sigil");
            var frostBoss=level.getEntitiesOfClass(FrostboundEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(BossEncounters.get(level).find(frostBoss.getUUID()).orElseThrow().type().equals("frostbound_colossus"),"Frostbound encounter type was not reserved");
            frostBoss.hurtServer(level,frostBoss.damageSources().genericKill(),10000);
            var iceDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(iceDrops.stream().anyMatch(item->item.getItem().is(BossContent.FROSTBOUND_TROPHY.get()))
                && iceDrops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Frostbound one-time regional rewards missing");
            var pine=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("pinewall_taiga/wolf_hollows")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),pine).left().isPresent(),"Skollvar biome fixture failed");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BossContent.SKOLLVAR_SIGIL.get()));
            helper.assertTrue(state.useItemOn(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS
                && owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"Skollvar altar summon failed or retained the sigil");
            var wolf=level.getEntitiesOfClass(SkollvarEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            wolf.hurtServer(level,wolf.damageSources().genericKill(),10000);
            var wolfDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(wolfDrops.stream().anyMatch(item->item.getItem().is(BossContent.SKOLLVAR_TROPHY.get()))
                && wolfDrops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("wintersteel").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Skollvar trophy or regional materials missing");
            var forest=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("elderwood/glowing_glades")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),forest).left().isPresent(),"Eldaroth biome fixture failed");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BossContent.ELDAROTH_SIGIL.get()));
            helper.assertTrue(state.useItemOn(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS
                && owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"Eldaroth altar summon failed or retained the sigil");
            var treant=level.getEntitiesOfClass(EldarothEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(BossEncounters.get(level).find(treant.getUUID()).orElseThrow().type().equals("eldaroth_treant"),"Eldaroth encounter type was not reserved");
            treant.hurtServer(level,treant.damageSources().genericKill(),10000);
            var treantDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(treantDrops.stream().anyMatch(item->item.getItem().is(BossContent.ELDAROTH_TROPHY.get()))
                && treantDrops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Eldaroth trophy or regional materials missing");
            var heartlands=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("heartlands/farmland_plains")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),heartlands).left().isPresent(),"Fallen King biome fixture failed");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BossContent.FALLEN_KING_SIGIL.get()));
            helper.assertTrue(state.useItemOn(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS
                && owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"Fallen King altar summon failed or retained the sigil");
            var fallen=level.getEntitiesOfClass(FallenKingEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(BossEncounters.get(level).find(fallen.getUUID()).orElseThrow().type().equals("fallen_king"),"Fallen King encounter type was not reserved");
            fallen.hurtServer(level,fallen.damageSources().genericKill(),10000);
            var kingDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(kingDrops.stream().anyMatch(item->item.getItem().is(BossContent.FALLEN_KING_TROPHY.get()))
                && kingDrops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Fallen King trophy or regional materials missing");
            var badlands=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/redrock_badlands")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),badlands).left().isPresent(),"Kragmor biome fixture failed");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BossContent.KRAGMOR_SIGIL.get()));
            helper.assertTrue(state.useItemOn(owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS
                && owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"Kragmor altar summon failed or retained the sigil");
            var warlord=level.getEntitiesOfClass(KragmorEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(BossEncounters.get(level).find(warlord.getUUID()).orElseThrow().type().equals("kragmor_canyon_warlord"),"Kragmor encounter type was not reserved");
            warlord.hurtServer(level,warlord.damageSources().genericKill(),10000);
            var warlordDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(warlordDrops.stream().anyMatch(item->item.getItem().is(BossContent.KRAGMOR_TROPHY.get()))
                && warlordDrops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Kragmor trophy or regional materials missing");
            helper.succeed();
        }finally { rejectSpawnAt=null; level.getServer().getPlayerList().remove(owner); }
    }
    private static void noctharDeepEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(1536,34,1536));var arena=altar.north(6);
        helper.assertTrue(altar.getY()>=level.getMinY()&&altar.getY()<=-6,"Nocthar test fixture is outside the permitted deep Y band");
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());
        var sigil=new net.minecraft.world.item.ItemStack(BossContent.NOCTHAR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,
                "Nocthar altar consumed sigil outside its registered deep biome");
            var sanctuary=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("umbral_sanctuary")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),sanctuary).left().isPresent(),"Nocthar biome fixture failed");
            helper.assertTrue(BossEncounterService.matchesBiome(level,altar,"nocthar_void_sovereign")&&BossEncounterService.matchesBiome(level,altar.above(),"nocthar_void_sovereign"),
                "Nocthar's protected sanctuary biome route was rejected");
            rejectSpawnAt=arena;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1
                &&!BossEncounters.get(level).conflicts(arena),"failed deep spawn consumed Nocthar sigil or leaked reservation");
            rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),
                "valid deep altar summon failed or did not consume one sigil");
            var boss=level.getEntitiesOfClass(NoctharEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Nocthar summon reported success without a live boss entity"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow(()->new IllegalStateException("Nocthar encounter reservation missing after summon")).type().equals("nocthar_void_sovereign"),"Nocthar encounter type was not persisted");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());boss.saveWithoutId(saved);
            var replay=BossRegistries.NOCTHAR.get().create(level,EntitySpawnReason.LOAD);replay.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get())).mapToInt(item->item.getItem().getCount()).sum()==8
                &&drops.stream().anyMatch(item->item.getItem().is(BossContent.NOCTHAR_TROPHY.get())),"Nocthar guaranteed rewards missing");
            long rare=drops.stream().filter(item->item.getItem().is(com.pipican.resource.RareResources.MATERIALS.get("voidsteel").get())).mapToInt(item->item.getItem().getCount()).sum();
            helper.assertTrue(rare==(Math.floorMod(boss.getUUID().hashCode(),5)==0?1:0),"Nocthar Voidsteel 20-percent encounter roll differed");
            replay.hurtServer(level,replay.damageSources().genericKill(),10000);
            drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("umbrite").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "reloaded defeated Nocthar duplicated rewards");
            helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void aetherionHighEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(1536,330,1536));var arena=altar.north(6);
        helper.assertTrue(altar.getY()>=251&&altar.getY()<=443,"Aetherion test altar is outside the valid high-altitude band");
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());
        var sigil=new net.minecraft.world.item.ItemStack(BossContent.AETHERION_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,
                "Aetherion altar consumed a sigil outside the Aetheris biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("aetheris_sanctuary")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Aetherion biome fixture failed");
            helper.assertTrue(BossEncounterService.matchesBiome(level,altar,"aetherion_sky_guardian")&&BossEncounterService.matchesBiome(level,altar.above(),"aetherion_sky_guardian"),
                "Aetherion sanctuary biome route was rejected at high altitude");
            rejectSpawnAt=arena;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL
                &&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Aetherion spawn consumed sigil or leaked its reservation");
            rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),
                "valid high-altitude Aetherion summon failed");
            var boss=level.getEntitiesOfClass(AetherionEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(boss.isNoGravity()&&boss.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
                &&BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("aetherion_sky_guardian"),"Aetherion did not spawn with flight navigation and a saved encounter binding");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.AETHERION_TROPHY.get()))
                &&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Aetherion guaranteed trophy/skycrystal rewards missing");
            long rare=drops.stream().filter(item->item.getItem().is(com.pipican.resource.RareResources.MATERIALS.get("celestium").get())).mapToInt(item->item.getItem().getCount()).sum();
            helper.assertTrue(rare==(Math.floorMod(boss.getUUID().hashCode(),4)==0?1:0),"Aetherion Celestium 25-percent encounter roll differed");
            helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void vermillionCrimsonEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(2048,32,2048));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.VERMILLION_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,
                "Vermillion altar accepted a non-Crimson biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("crimson_wilds")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Crimson Wilds biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL
                &&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Vermillion spawn consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Crimson altar summon failed");
            var boss=level.getEntitiesOfClass(VermillionEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Vermillion spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("vermillion_blood_monarch"),"Vermillion encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.VERMILLION_TROPHY.get()))
                &&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Vermillion guaranteed trophy/bloodstone rewards missing");
            long rare=drops.stream().filter(item->item.getItem().is(com.pipican.resource.RareResources.MATERIALS.get("bloodcrystal").get())).mapToInt(item->item.getItem().getCount()).sum();
            helper.assertTrue(rare==(Math.floorMod(boss.getUUID().hashCode(),4)==0?1:0),"Vermillion Bloodcrystal encounter roll differed");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void longweiJadeEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(2560,32,2560));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.LONGWEI_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Longwei altar accepted a non-Jade biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/jade_empire")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Jade Empire biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1
                &&!BossEncounters.get(level).conflicts(arena),"rejected Longwei spawn consumed its sigil or leaked its reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Jade altar summon failed");
            var boss=level.getEntitiesOfClass(LongweiEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Longwei spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("longwei_jade_dragon"),"Longwei encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.LONGWEI_TROPHY.get()))
                &&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get())).mapToInt(item->item.getItem().getCount()).sum()==8,
                "Longwei guaranteed jade scale/essence rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void morgathaMarshfenEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(3072,32,3072));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.MUD_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.MORGATHA_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Morgatha altar accepted a non-Marshfen biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/marshfen")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Marshfen biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Morgatha spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Marshfen altar summon failed");
            var boss=level.getEntitiesOfClass(MorgathaEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Morgatha spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("morgatha_swamp_queen"),"Morgatha encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.MORGATHA_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Morgatha trophy/mirestone rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void aureliosInnerSeaEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(3328,32,3328));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.AURELIOS_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Aurelios altar accepted a non-Inner Sea biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/inner_sea")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Inner Sea biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Aurelios spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Inner Sea altar summon failed");
            var boss=level.getEntitiesOfClass(AureliosEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Aurelios spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("aurelios_sunken_emperor"),"Aurelios encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.AURELIOS_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Aurelios trophy/pearlsilver rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void lianhuaLotusEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(3584,32,3584));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.LIANHUA_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Lianhua altar accepted a non-Lotus Delta biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/lotus_delta")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Lotus Delta biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Lianhua spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Lotus Delta altar summon failed");
            var boss=level.getEntitiesOfClass(LianhuaEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Lianhua spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("lianhua_lotus_serpent"),"Lianhua encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.LIANHUA_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lotus_opal").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Lianhua trophy/lotus opal rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void akhmarSunEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(4096,32,4096));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);
        var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.AKHMAR_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Akhmar altar accepted a non-Sunscorch biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/sunscorch_desert")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Sunscorch Desert biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Akhmar spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Sunscorch altar summon failed");
            var boss=level.getEntitiesOfClass(AkhmarEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Akhmar spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("akhmar_sand_emperor"),"Akhmar encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.AKHMAR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Akhmar trophy/Sunglass rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void akhmarCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(50000,30,50000));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))
            level.setBlock(pos,pos.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var front=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        level.addFreshEntity(front);level.addFreshEntity(refuge);
        var boss=BossRegistries.AKHMAR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+12,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;refuge.snapTo(origin.getX()+6,origin.getY(),origin.getZ());refreshMockPositions(level,front,refuge);
            var procession=new AkhmarCombat(boss);helper.assertTrue(procession.begin(AkhmarCombat.PROCESSION,front),"Akhmar dune procession failed to start");front.snapTo(origin.getX()+6,origin.getY(),origin.getZ()+4.2);refreshMockPositions(level,front,refuge);
            for(int i=0;i<40;i++)procession.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Akhmar procession struck before its marked-lane warning");for(int i=0;i<16;i++)procession.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Akhmar winding procession or safe-lane counterplay failed: front="+front.getHealth()+", refuge="+refuge.getHealth()+", frontPos="+front.position()+", refugePos="+refuge.position()+", inside="+boss.insideArena(front.position())+"/"+boss.insideArena(refuge.position())+", nearby="+level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,boss.getBoundingBox().inflate(26)).size()+", active="+procession.clock().active()+", remaining="+procession.clock().remaining()+", resolved="+procession.clock().resolved());
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;
            var obelisks=new AkhmarCombat(boss);helper.assertTrue(obelisks.begin(AkhmarCombat.OBELISKS,front),"Akhmar sun obelisks failed to start");var shadow=obelisks.shadowPosition(0);refuge.snapTo(shadow.x,shadow.y,shadow.z);
            for(int i=0;i<49;i++)obelisks.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Akhmar solar pulse resolved before the safe shadows");obelisks.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Akhmar stable obelisk shadow counterplay failed");
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;
            var brokenObelisk=new AkhmarCombat(boss);helper.assertTrue(brokenObelisk.begin(AkhmarCombat.OBELISKS,front),"Akhmar interruptible obelisk attack failed to start");var shot=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);
            helper.assertTrue(shot!=null,"Akhmar obelisk interrupt projectile could not be created");shot.setPos(origin.getX()+6,origin.getY(),origin.getZ());shot.setNoGravity(true);shot.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);level.addFreshEntity(shot);
            var exposedShadow=brokenObelisk.shadowPosition(0);refuge.snapTo(exposedShadow.x,exposedShadow.y,exposedShadow.z);for(int i=0;i<50;i++)brokenObelisk.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()<20&&!shot.isAlive(),"breaking a sun obelisk did not remove its safe shadow");
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;refuge.snapTo(origin.getX()-8,origin.getY(),origin.getZ());
            var court=new AkhmarCombat(boss);helper.assertTrue(court.begin(AkhmarCombat.COURT,front),"Akhmar buried court failed to start");var marked=court.eruptionPosition(0);front.snapTo(marked.x,marked.y,marked.z);
            for(int i=0;i<59;i++)court.tick(level);helper.assertTrue(front.getHealth()==20,"Akhmar eruption struck before its silhouette warning");court.tick(level);court.tick(level);court.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Akhmar sequential eruption silhouette/dodge failed: front="+front.getHealth()+", refuge="+refuge.getHealth()+", marked="+marked+", player="+front.position()+", inside="+boss.insideArena(front.position())+", remaining="+court.clock().remaining()+", active="+court.clock().active()+", resolved="+court.clock().resolved()+", progress="+court.eruptionProgress());
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());var phaseTwo=new AkhmarCombat(boss);
            helper.assertTrue(phaseTwo.updatePhase(450,900)&&phaseTwo.begin(AkhmarCombat.PROCESSION,front),"Akhmar phase-two lane shift failed to start");var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());phaseTwo.save(saved);
            var reloaded=new AkhmarCombat(boss);reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));
            helper.assertTrue(reloaded.clock().phase()==2&&reloaded.clock().active().equals(AkhmarCombat.PROCESSION),"Akhmar phase/lane attack state was lost on reload");helper.succeed();
        }finally{front.discard();refuge.discard();}
    }
    private static void netharisRiverEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(4608,32,4608));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.NETHARIS_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Netharis altar accepted a non-Riverlands biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/riverlands")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Riverlands biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Netharis spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Riverlands altar summon failed");
            var boss=level.getEntitiesOfClass(NetharisEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Netharis spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("netharis_river_guardian"),"Netharis encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.NETHARIS_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Netharis trophy/Sunstone rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void netharisCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(53000,30,53000));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(pos,pos.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var front=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(front);level.addFreshEntity(refuge);
        var boss=BossRegistries.NETHARIS.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+12,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;
            var gates=new NetharisCombat(boss);helper.assertTrue(gates.begin(NetharisCombat.GATES,front),"Netharis river gates failed to start");var crossing=gates.safeGatePosition(0);refuge.snapTo(crossing.x,crossing.y,crossing.z);front.snapTo(origin.getX()+4,origin.getY(),origin.getZ()-5);
            for(int i=0;i<40;i++)gates.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Netharis gate struck before its marked crossing");gates.tick(level);gates.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Netharis moving water gate or marked-opening counterplay failed");
            front.setHealth(20);front.invulnerableTime=0;var spears=new NetharisCombat(boss);helper.assertTrue(spears.begin(NetharisCombat.JAVELINS,front),"Netharis silt javelins failed to start");var first=spears.javelinPosition(0);front.snapTo(first.x,first.y,first.z);
            for(int i=0;i<50;i++)spears.tick(level);helper.assertTrue(front.getHealth()==20,"Netharis javelin struck before its warning ended");spears.tick(level);spears.tick(level);
            helper.assertTrue(front.getHealth()<20&&spears.javelinProgress()==1,"first sequential Netharis javelin failed");front.setHealth(20);front.invulnerableTime=0;var second=spears.javelinPosition(1);front.snapTo(second.x,second.y,second.z);
            for(int i=0;i<9;i++)spears.tick(level);helper.assertTrue(front.getHealth()<20&&spears.javelinProgress()==2,"numbered javelin sequence did not advance independently");
            front.setHealth(20);front.invulnerableTime=0;refuge.setHealth(20);refuge.invulnerableTime=0;var current=new NetharisCombat(boss);helper.assertTrue(current.begin(NetharisCombat.TRIBUTARY,front),"Netharis tributary call failed to start");var safe=current.safeCrossingPosition();refuge.snapTo(safe.x,safe.y,safe.z);front.snapTo(origin.getX()+8,origin.getY(),origin.getZ()-5);
            for(int i=0;i<60;i++)current.tick(level);helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Netharis surge unlit-crossing counterplay failed");
            front.setHealth(20);front.invulnerableTime=0;var interrupted=new NetharisCombat(boss);helper.assertTrue(interrupted.updatePhase(425,850)&&interrupted.begin(NetharisCombat.TRIBUTARY,front),"Netharis phase-two call did not begin");var rotated=interrupted.safeCrossingPosition();helper.assertTrue(rotated.distanceToSqr(safe)>1,"Netharis phase-two crossing failed to rotate");
            for(int i=0;i<3;i++){var shot=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(shot!=null,"Netharis anchor interrupt projectile could not be created");var p=interrupted.anchorPosition(i);shot.setPos(p.x,p.y,p.z);shot.setNoGravity(true);shot.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);level.addFreshEntity(shot);}
            interrupted.tick(level);helper.assertTrue(interrupted.brokenAnchors()==3,"projectiles did not interrupt all tributary anchors");var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());interrupted.save(saved);
            var reloaded=new NetharisCombat(boss);reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));for(int i=0;i<59;i++)reloaded.tick(level);
            helper.assertTrue(reloaded.clock().phase()==2&&reloaded.brokenAnchors()==3&&front.getHealth()==20&&reloaded.exposureTicks()>0,"phase-two interrupted surge or anchor exposure did not persist");helper.succeed();
        }finally{front.discard();refuge.discard();boss.discard();}
    }
    private static void zahiraSpiceEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(5120,32,5120));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))
            level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.ZAHIRA_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1,"Zahira altar accepted a non-Spice Coast biome");
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/spice_coast")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Spice Coast biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Zahira spawn consumed its sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Spice Coast altar summon failed");
            var boss=level.getEntitiesOfClass(ZahiraEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Zahira spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("zahira_crimson_corsair"),"Zahira encounter type was not saved");
            boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.ZAHIRA_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("saffron_gold").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Zahira trophy/Saffron Gold rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void zahiraCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(57000,30,57000));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(pos,pos.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.CUT_SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var front=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(front);level.addFreshEntity(refuge);
        var boss=BossRegistries.ZAHIRA.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+12,origin.getY(),origin.getZ());var boarding=new ZahiraCombat(boss);helper.assertTrue(boarding.begin(ZahiraCombat.BOARDING,front),"Zahira boarding lines failed to begin");var landing=boarding.boardingLandingPosition(0);front.snapTo(landing.x,landing.y,landing.z);refuge.snapTo(origin.getX()+7,origin.getY(),origin.getZ()-2.4);
            for(int i=0;i<40;i++)boarding.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Zahira boarding route landed before warning");boarding.tick(level);boarding.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20&&boarding.boardingProgress()==1,"marked boarding landing or sidestep counterplay failed");
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+4,origin.getY(),origin.getZ());refuge.setHealth(20);refuge.invulnerableTime=0;refuge.snapTo(origin.getX()+4,origin.getY(),origin.getZ()+2.4);
            var broadside=new ZahiraCombat(boss);helper.assertTrue(broadside.begin(ZahiraCombat.BROADSIDE,front),"Zahira broadside failed to start");var shot=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(shot!=null,"Zahira cannon interrupt arrow failed to create");var gun=broadside.cannonPosition(0);shot.setPos(gun.x,gun.y,gun.z);shot.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);shot.setNoGravity(true);level.addFreshEntity(shot);broadside.tick(level);helper.assertTrue(broadside.disabledCannons()==1&&!shot.isAlive(),"lit broadside cannon was not interruptible");
            for(int i=0;i<48;i++)broadside.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Zahira broadside fired before the lane warning");broadside.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20,"Zahira interrupted cannon lane or between-barrel refuge failed");
            front.setHealth(20);front.invulnerableTime=0;var fuses=new ZahiraCombat(boss);helper.assertTrue(fuses.begin(ZahiraCombat.FUSES,front),"Zahira fuse dance failed to start");var bomb=fuses.bombPosition(0);front.snapTo(bomb.x,bomb.y,bomb.z);refuge.setHealth(20);refuge.invulnerableTime=0;refuge.snapTo(bomb.x+4.2,bomb.y,bomb.z+2);
            for(int i=0;i<60;i++)fuses.tick(level);helper.assertTrue(fuses.fuse(0)==16&&front.getHealth()==20,"Zahira fuse did not start at the announced detonation time");for(int i=0;i<15;i++)fuses.tick(level);helper.assertTrue(front.getHealth()==20,"Zahira first bomb exploded before its individual fuse ended");fuses.tick(level);
            helper.assertTrue(front.getHealth()<20&&refuge.getHealth()==20&&fuses.activeBombs()==2,"fixed bomb circle or independent fuse timing failed");
            var kicked=new ZahiraCombat(boss);helper.assertTrue(kicked.updatePhase(350,700)&&kicked.begin(ZahiraCombat.FUSES,front),"Zahira phase-two fuse attack failed");for(int i=0;i<60;i++)kicked.tick(level);net.minecraft.world.phys.Vec3 original=kicked.bombPosition(0);var kick=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(kick!=null,"Zahira bomb-kick projectile failed to create");kick.setPos(original.x,original.y,original.z);kick.setDeltaMovement(new net.minecraft.world.phys.Vec3(0,0,1));kick.setNoGravity(true);level.addFreshEntity(kick);kicked.tick(level);
            helper.assertTrue(kicked.bombPosition(0).distanceToSqr(original)>9&&kicked.fuse(0)==15,"bomb kick failed to redirect the marked explosion");var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());kicked.save(saved);var restored=new ZahiraCombat(boss);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));
            helper.assertTrue(restored.clock().phase()==2&&restored.bombPosition(0).distanceToSqr(original)>9&&restored.fuse(0)==15,"phase, kicked bomb location or fuse clock was lost on reload");helper.succeed();
        }finally{front.discard();refuge.discard();boss.discard();}
    }
    private static void xaltekEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(6400,32,6400));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.XALTEK_SIGIL.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{
            var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/emerald_jungle")));
            helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"emerald jungle biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Xaltek spawn consumed the sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"emerald jungle altar summon failed");
            var boss=level.getEntitiesOfClass(XaltekEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Xaltek spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("xaltek_emerald_devourer"),"Xaltek encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));
            helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.XALTEK_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Xaltek trophy/Verdantite rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void xaltekCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(59000,30,59000));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(pos,pos.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var front=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(front);level.addFreshEntity(refuge);
        var boss=BossRegistries.XALTEK.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{
            front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var tongue=new XaltekCombat(boss);helper.assertTrue(tongue.begin(XaltekCombat.TONGUE,front),"Xaltek tongue line did not start");
            for(int i=0;i<49;i++)tongue.tick(level);helper.assertTrue(front.getHealth()==20,"Xaltek tongue struck before its warning");var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Xaltek tongue interrupt projectile missing");arrow.setPos(front.getX(),front.getY()+0.5,front.getZ());arrow.setNoGravity(true);level.addFreshEntity(arrow);tongue.tick(level);helper.assertTrue(front.getHealth()==20&&!arrow.isAlive(),"exposed Xaltek tongue tip was not interruptible");
            var phase=new XaltekCombat(boss);helper.assertTrue(phase.updatePhase(475,950)&&phase.begin(XaltekCombat.SNARES,front),"Xaltek phase-two snare failed to begin");
            for(int i=0;i<3;i++){var pod=phase.podPosition(i);var shot=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(shot!=null,"Xaltek seed pod projectile missing");shot.setPos(pod.x,pod.y,pod.z);shot.setNoGravity(true);level.addFreshEntity(shot);}
            phase.tick(level);helper.assertTrue(phase.brokenPods()==3,"Xaltek phase-two seed pods could not be broken");for(int i=0;i<38;i++)phase.tick(level);helper.assertTrue(front.getHealth()==20,"Xaltek snare struck before its telegraph ended");phase.tick(level);
            helper.assertTrue(front.getHealth()==20&&phase.exposureTicks()==70&&phase.damageMultiplier()>1f,"broken snare counterplay did not expose Xaltek's phase-two weak spot: hp="+front.getHealth()+", exposed="+phase.exposureTicks()+", multiplier="+phase.damageMultiplier()+", broken="+phase.brokenPods()+", phase="+phase.clock().phase()+", active="+phase.clock().active()+", remaining="+phase.clock().remaining());
            front.snapTo(origin.getX()+12,origin.getY(),origin.getZ()+3);refuge.snapTo(origin.getX()+2,origin.getY(),origin.getZ()+1);var pounce=new XaltekCombat(boss);helper.assertTrue(pounce.begin(XaltekCombat.POUNCE,front),"Xaltek canopy pounce failed to start");var landing=pounce.landing();front.snapTo(landing.x+3.2,landing.y,landing.z+2.4);front.setHealth(20);front.invulnerableTime=0;refuge.setHealth(20);refuge.invulnerableTime=0;refuge.snapTo(landing.x,landing.y,landing.z);
            for(int i=0;i<59;i++)pounce.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()==20,"Xaltek canopy pounce resolved before the shadow warning");pounce.tick(level);helper.assertTrue(front.getHealth()==20&&refuge.getHealth()<20,"Xaltek locked landing circle or sidestep counterplay failed: front="+front.getHealth()+", refuge="+refuge.getHealth()+", refugePos="+refuge.position()+", landing="+landing+", active="+pounce.clock().active()+", remaining="+pounce.clock().remaining());
            var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());phase.save(save);var restored=new XaltekCombat(boss);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(save.buildResult())));helper.assertTrue(restored.clock().phase()==2&&restored.exposureTicks()==70&&restored.brokenPods()==3,"Xaltek phase-two pod exposure was lost on reload");helper.succeed();
        }finally{front.discard();refuge.discard();boss.discard();}
    }
    private static void corallithEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(6800,32,6800));var arena=altar.north(6);
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(pos,pos.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.CORALLITH_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/coral_isles")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"coral isles biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Corallith spawn consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"coral isles altar summon failed");var boss=level.getEntitiesOfClass(CorallithEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Corallith spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("corallith_reef_colossus"),"Corallith encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.CORALLITH_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Corallith trophy/Tidalite reward missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void corallithCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(61000,30,61000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(player);level.addFreshEntity(refuge);var boss=BossRegistries.CORALLITH.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{player.setHealth(20);player.invulnerableTime=0;player.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var gates=new CorallithCombat(boss);helper.assertTrue(gates.begin(CorallithCombat.GATES,player),"Corallith coral gates failed to start");var safe=gates.gateOpening();refuge.snapTo(origin.getX()-6,origin.getY(),origin.getZ());player.snapTo(safe.x,safe.y,safe.z);for(int i=0;i<39;i++)gates.tick(level);helper.assertTrue(player.getHealth()==20&&refuge.getHealth()==20,"Corallith gate ring struck before warning");gates.tick(level);helper.assertTrue(player.getHealth()==20&&refuge.getHealth()<20,"Corallith gate had no stable refuge opening");
            player.setHealth(20);player.invulnerableTime=0;var echo=new CorallithCombat(boss);helper.assertTrue(echo.updatePhase(500,1000)&&echo.begin(CorallithCombat.ECHO,player),"Corallith phase-two reef echo failed to start");for(int i=0;i<50;i++)echo.tick(level);var target=echo.echoTarget();player.snapTo(target.x,target.y,target.z);for(int i=0;i<13;i++)echo.tick(level);helper.assertTrue(player.getHealth()==20,"Corallith sonar echo struck before its separate warning finished");var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());echo.save(saved);var restored=new CorallithCombat(boss);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));helper.assertTrue(restored.clock().phase()==2,"Corallith phase lost on echo reload");restored.tick(level);helper.assertTrue(player.getHealth()<20,"saved announced reef echo did not resolve at its target");
            player.setHealth(20);player.invulnerableTime=0;var urchins=new CorallithCombat(boss);helper.assertTrue(urchins.begin(CorallithCombat.URCHINS,player),"Corallith urchin fan failed to start");for(int i=0;i<59;i++)urchins.tick(level);helper.assertTrue(player.getHealth()==20,"Corallith urchins advanced before warning");urchins.tick(level);var pos=urchins.socket(0);var shot=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(shot!=null,"Corallith urchin interrupt projectile failed");shot.setPos(pos.x,pos.y,pos.z);shot.setNoGravity(true);level.addFreshEntity(shot);urchins.tick(level);helper.assertTrue(urchins.destroyedUrchins()==1&&!shot.isAlive(),"previewed rolling urchin was not shootable");helper.succeed();
        }finally{player.discard();refuge.discard();boss.discard();}
    }
    private static void azharEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(7200,32,7200));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.AZHAR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/goldgrass_savanna")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"goldgrass savanna biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Azhar summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"goldgrass savanna altar summon failed");var boss=level.getEntitiesOfClass(AzharEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Azhar spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("azhar_golden_behemoth"),"Azhar encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.AZHAR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Azhar trophy/Dawnbronze rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void azharCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(62000,30,62000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var safePlayer=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(safePlayer);level.addFreshEntity(danger);var boss=BossRegistries.AZHAR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{safePlayer.setHealth(20);safePlayer.invulnerableTime=0;safePlayer.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var stampede=new AzharCombat(boss);helper.assertTrue(stampede.begin(AzharCombat.STAMPEDE,safePlayer),"Azhar stampede failed to start");var safe=stampede.safeStampedePosition();safePlayer.snapTo(safe.x,safe.y,safe.z);danger.snapTo(origin.getX()+5,origin.getY(),origin.getZ()-4.5);for(int i=0;i<49;i++)stampede.tick(level);helper.assertTrue(safePlayer.getHealth()==20&&danger.getHealth()==20,"Azhar stampede crossed lanes before warning ended");stampede.tick(level);helper.assertTrue(safePlayer.getHealth()==20&&danger.getHealth()<20,"Azhar stampede lacked a stable clear lane");
            var rays=new AzharCombat(boss);helper.assertTrue(rays.updatePhase(550,1100)&&rays.begin(AzharCombat.REFRACTION,safePlayer),"Azhar phase-two refraction failed to begin");var plate=rays.platePosition(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Azhar plate interrupt projectile missing");arrow.setPos(plate.x,plate.y,plate.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);rays.tick(level);helper.assertTrue(rays.brokenPlates()==1&&!arrow.isAlive(),"Azhar illuminated sun plate was not breakable");var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());rays.save(saved);var restored=new AzharCombat(boss);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));helper.assertTrue(restored.clock().phase()==2&&restored.brokenPlates()==1,"Azhar plate break or phase was lost on reload");
            safePlayer.setHealth(20);safePlayer.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var kneel=new AzharCombat(boss);helper.assertTrue(kneel.updatePhase(550,1100)&&kneel.begin(AzharCombat.KNEEL,safePlayer),"Azhar phase-two kneel failed");safePlayer.snapTo(kneel.lobeTarget(3).x,origin.getY(),kneel.lobeTarget(3).z);danger.snapTo(origin.getX(),origin.getY(),origin.getZ());for(int i=0;i<60;i++)kneel.tick(level);for(int i=0;i<9;i++)kneel.tick(level);helper.assertTrue(safePlayer.getHealth()==20&&danger.getHealth()==20,"Azhar staggered lobe struck before its individual warning");saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());kneel.save(saved);var reload=new AzharCombat(boss);reload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));reload.tick(level);helper.assertTrue(safePlayer.getHealth()<20&&danger.getHealth()==20&&reload.pendingLobeTicks()==12&&reload.lobePass()==1,"Azhar saved lobe, inner refuge or phase-two follow-up failed");helper.succeed();
        }finally{safePlayer.discard();danger.discard();boss.discard();}
    }
    private static void nhalkorrEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(7600,32,7600));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.NHALKORR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/northern_ice_sea")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"northern ice sea biome fixture failed");
            rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Nhalkorr summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"northern ice sea altar summon failed");var boss=level.getEntitiesOfClass(NhalkorrEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Nhalkorr spawn missing"));
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("nhalkorr_leviathan"),"Nhalkorr encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.NHALKORR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Nhalkorr trophy/Glacierite rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void nhalkorrCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(64000,30,64000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.NHALKORR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.setHealth(20);refuge.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var current=new NhalkorrCombat(boss);helper.assertTrue(current.begin(NhalkorrCombat.CURRENT,refuge),"Nhalkorr current failed to start");danger.snapTo(origin.getX()+5,origin.getY(),origin.getZ());for(int i=0;i<39;i++)current.tick(level);helper.assertTrue(danger.getHealth()==20,"Nhalkorr current struck before its warning ended");current.tick(level);helper.assertTrue(danger.getHealth()<20,"Nhalkorr marked current lane failed to resolve");
            var interrupted=new NhalkorrCombat(boss);helper.assertTrue(interrupted.begin(NhalkorrCombat.CURRENT,refuge),"Nhalkorr anchor current failed to restart");var anchor=interrupted.anchorPosition(1);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Nhalkorr anchor interrupt projectile missing");arrow.setPos(anchor.x,anchor.y,anchor.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);interrupted.tick(level);helper.assertTrue(interrupted.brokenAnchors()==1&&!arrow.isAlive(),"Nhalkorr current anchor was not interruptible");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var undertow=new NhalkorrCombat(boss);helper.assertTrue(undertow.updatePhase(550,1100)&&undertow.begin(NhalkorrCombat.UNDERTOW,refuge),"Nhalkorr phase-two undertow failed");var safe=undertow.safeCurrentPosition();refuge.snapTo(safe.x,safe.y,safe.z);var vortex=undertow.vortexPosition(0);danger.snapTo(vortex.x+2,vortex.y,vortex.z);for(int i=0;i<49;i++)undertow.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Nhalkorr vortex resolved before telegraph");undertow.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Nhalkorr displayed safe current/refuge failed: refuge="+refuge.getHealth()+", danger="+danger.getHealth()+", safe="+safe+", refugePos="+refuge.position()+", vortex="+vortex+", dangerPos="+danger.position()+", inside="+boss.insideArena(refuge.position())+"/"+boss.insideArena(danger.position())+", nearby="+level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,boss.getBoundingBox().inflate(28)).size()+", active="+undertow.clock().active()+", remaining="+undertow.clock().remaining()+", resolved="+undertow.clock().resolved());
            var breaches=new NhalkorrCombat(boss);helper.assertTrue(breaches.updatePhase(550,1100)&&breaches.begin(NhalkorrCombat.BREACHES,refuge),"Nhalkorr ink breach failed to start");for(int i=0;i<60;i++)breaches.tick(level);for(int i=0;i<2;i++)breaches.tick(level);var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());breaches.save(saved);var reload=new NhalkorrCombat(boss);reload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(saved.buildResult())));helper.assertTrue(reload.breachIndex()==1&&reload.pendingBreachTicks()==10,"Nhalkorr staggered breach timer/index failed reload");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void thalassorEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(86000,32,86000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.THALASSOR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/western_ocean")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"western ocean biome fixture failed");rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Thalassor summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"western ocean altar summon failed");var boss=level.getEntitiesOfClass(ThalassorEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Thalassor spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("thalassor_deep_tyrant"),"Thalassor encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.THALASSOR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Thalassor trophy/Tideglass rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void thalassorCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(72000,30,72000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,6,16)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.THALASSOR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var fan=new ThalassorCombat(boss);helper.assertTrue(fan.begin(ThalassorCombat.FAN,refuge),"Thalassor trident fan failed to start");var safe=fan.fanPoint(fan.safeGap(),8);refuge.snapTo(safe.x,safe.y,safe.z);var mark=fan.fanPoint(0,8);danger.snapTo(mark.x,mark.y,mark.z);refreshMockPositions(level,refuge,danger);for(int i=0;i<39;i++)fan.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Thalassor tridents struck before fan warning");fan.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Thalassor rotating trident fan gap failed");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+13);danger.snapTo(origin.getX()+5,origin.getY(),origin.getZ());var pressure=new ThalassorCombat(boss);helper.assertTrue(pressure.begin(ThalassorCombat.PRESSURE,danger),"Thalassor pressure bubbles failed to start");for(int i=0;i<49;i++)pressure.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Thalassor pressure field burst before its boundary filled");pressure.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Thalassor expanding pressure field escape boundary failed");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var mines=new ThalassorCombat(boss);helper.assertTrue(mines.begin(ThalassorCombat.MINES,refuge),"Thalassor abyssal mine chain failed to start");var source=mines.mine(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Thalassor source mine interrupt projectile missing");arrow.setPos(source.x,source.y,source.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);mines.tick(level);helper.assertTrue(mines.brokenMines()==1&&!arrow.isAlive(),"Thalassor lit source mine was not interruptible");refuge.snapTo(origin.getX(),origin.getY(),origin.getZ());var linked=mines.chainPoint(1);danger.snapTo(linked.x,linked.y,linked.z);for(int i=0;i<59;i++)mines.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Thalassor mines detonated before chain warning completed");mines.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Thalassor source mine interruption did not suppress its linked chain");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void tengriEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(84000,32,84000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.TENGRI_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/khagan_steppe")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Khagan steppe biome fixture failed");rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Tengri summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Khagan steppe altar summon failed");var boss=level.getEntitiesOfClass(TengriEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Tengri spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("tengri_champion"),"Tengri encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.TENGRI_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Tengri trophy/Skyiron rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void tengriCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(70000,30,70000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.TENGRI.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var charge=new TengriCombat(boss);helper.assertTrue(charge.begin(TengriCombat.TRIANGLE,refuge),"Tengri triangle charge failed to start");var edge=charge.triangleEdgePoint(0);danger.snapTo(edge.x,edge.y,edge.z);refreshMockPositions(level,refuge,danger);for(int i=0;i<39;i++)charge.tick(level);helper.assertTrue(danger.getHealth()==20,"Tengri triangle struck before warning");charge.tick(level);helper.assertTrue(danger.getHealth()<20&&charge.chargeIndex()==1,"Tengri first announced triangle dash did not strike");for(int i=0;i<5;i++)charge.tick(level);helper.assertTrue(charge.chargeIndex()==1,"Tengri triangle dashes were not staggered");charge.tick(level);helper.assertTrue(charge.chargeIndex()==2,"Tengri second triangle dash did not resolve");for(int i=0;i<6;i++)charge.tick(level);helper.assertTrue(charge.chargeIndex()==3,"Tengri final triangle dash did not resolve");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var volley=new TengriCombat(boss);helper.assertTrue(volley.begin(TengriCombat.VOLLEY,refuge),"Tengri falcon volley failed");var safe=volley.fanPoint(volley.safeGap(),20);refuge.snapTo(safe.x,safe.y,safe.z);var marked=volley.fanPoint(0,20);danger.snapTo(marked.x,marked.y,marked.z);for(int i=0;i<49;i++)volley.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Tengri fan struck before warning");volley.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Tengri volley gap counterplay failed: gap="+volley.safeGap()+", refuge="+refuge.getHealth()+"/invul="+refuge.invulnerableTime+" at "+refuge.position()+", danger="+danger.getHealth()+"/invul="+danger.invulnerableTime+" at "+danger.position()+", marked="+marked+", safe="+safe+", inside="+boss.insideArena(danger.position())+"/"+boss.insideArena(refuge.position())+", nearby="+level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,boss.getBoundingBox().inflate(28)).size()+", active="+volley.clock().active()+", remaining="+volley.clock().remaining()+", resolved="+volley.clock().resolved()+", end="+volley.fanPoint(0,99));
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var rally=new TengriCombat(boss);helper.assertTrue(rally.updatePhase(375,750)&&rally.begin(TengriCombat.BANNER,refuge),"Tengri phase-two banner rally failed");var banner=rally.banner(1);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Tengri banner interrupt projectile missing");arrow.setPos(banner.x,banner.y,banner.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);rally.tick(level);helper.assertTrue(rally.brokenBanners()==1&&!arrow.isAlive(),"Tengri rally banner was not breakable");refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+3);danger.snapTo(origin.getX()+5,origin.getY(),origin.getZ()+3);for(int i=0;i<58;i++)rally.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Tengri rally resolved before warning");rally.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Tengri interrupted rally refuge/counterplay failed");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void valthorEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(82000,32,82000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.VALTHOR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/stormcoast")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Stormcoast biome fixture failed");rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Valthor summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Stormcoast altar summon failed");var boss=level.getEntitiesOfClass(ValthorEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Valthor spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("valthor_stormbringer"),"Valthor encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.VALTHOR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Valthor trophy/Thunderbasalt rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void valthorCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(68000,30,68000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.VALTHOR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var chains=new ValthorCombat(boss);helper.assertTrue(chains.begin(ValthorCombat.CONDUCTORS,refuge),"Valthor conductor attack failed");var anchor=chains.conductor(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Valthor conductor interrupt projectile missing");arrow.setPos(anchor.x,anchor.y,anchor.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);chains.tick(level);helper.assertTrue(chains.brokenConductors()==1&&!arrow.isAlive(),"Valthor conductor was not interruptible");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ());danger.snapTo(origin.getX()+6,origin.getY(),origin.getZ());var ring=new ValthorCombat(boss);helper.assertTrue(ring.begin(ValthorCombat.DONUT,danger),"Valthor thunder annulus failed to start");for(int i=0;i<49;i++)ring.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Valthor annulus struck before warning ended");ring.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Valthor center-safe thunder annulus did not resolve");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var gale=new ValthorCombat(boss);helper.assertTrue(gale.updatePhase(425,850)&&gale.begin(ValthorCombat.GALES,refuge),"Valthor phase-two gale lanes failed");var safeGale=gale.safeGalePosition();refuge.snapTo(safeGale.x,safeGale.y,safeGale.z);var unsafeGale=gale.galeLanePosition(gale.safeLane()==0?1:0);danger.snapTo(unsafeGale.x,unsafeGale.y,unsafeGale.z);for(int i=0;i<59;i++)gale.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Valthor gales struck before warning ended");gale.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Valthor lit gale lane or safe-channel counterplay failed: safe="+refuge.getHealth()+", marked="+danger.getHealth());helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void hrothgarEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(8000,32,8000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.HROTHGAR_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/fjordmark")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"fjordmark biome fixture failed");rejectSpawnAt=arena;helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&sigil.getCount()==1&&!BossEncounters.get(level).conflicts(arena),"rejected Hrothgar summon consumed sigil or leaked reservation");rejectSpawnAt=null;
            helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"fjordmark altar summon failed");var boss=level.getEntitiesOfClass(HrothgarEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Hrothgar spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("hrothgar_drowned_jarl"),"Hrothgar encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.HROTHGAR_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Hrothgar trophy/Stormsilver rewards missing");helper.succeed();
        }finally{rejectSpawnAt=null;level.getServer().getPlayerList().remove(owner);}
    }
    private static void hrothgarCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(66000,30,66000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.HROTHGAR.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()-8);danger.snapTo(origin.getX()+3,origin.getY(),origin.getZ());var oath=new HrothgarCombat(boss);helper.assertTrue(oath.begin(HrothgarCombat.OATH,refuge),"Hrothgar oath failed to start");for(int i=0;i<39;i++)oath.tick(level);helper.assertTrue(danger.getHealth()==20,"Hrothgar oath struck before its telegraph");oath.tick(level);for(int i=0;i<11;i++)oath.tick(level);helper.assertTrue(danger.getHealth()==20&&oath.pendingOathTicks()==1,"Hrothgar return path resolved too early");oath.tick(level);helper.assertTrue(danger.getHealth()==20&&oath.pendingOathTicks()==0,"Hrothgar return tell did not leave its last reaction tick");oath.tick(level);helper.assertTrue(danger.getHealth()<20,"Hrothgar returning oath path failed to hit locked curve");
            var chains=new HrothgarCombat(boss);helper.assertTrue(chains.begin(HrothgarCombat.CHAINS,refuge),"Hrothgar chain attack failed to start");var anchor=chains.chainAnchor(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Hrothgar weak-link projectile missing");arrow.setPos(anchor.x,anchor.y,anchor.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);chains.tick(level);helper.assertTrue(chains.brokenLinks()==1&&!arrow.isAlive(),"Hrothgar highlighted chain link was not breakable");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var rowers=new HrothgarCombat(boss);helper.assertTrue(rowers.updatePhase(350,700)&&rowers.begin(HrothgarCombat.ROWERS,refuge),"Hrothgar phase-two rowing dead failed");var safe=rowers.safeRowPosition();refuge.snapTo(safe.x,safe.y,safe.z);int dangerousLane=rowers.safeGap()==1?2:1;var lane=rowers.rowLanePosition(dangerousLane);danger.snapTo(lane.x,lane.y,lane.z);var rower=rowers.rowerPosition(0);arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Hrothgar rower interrupt projectile missing");arrow.setPos(rower.x,rower.y,rower.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);rowers.tick(level);helper.assertTrue(rowers.brokenRowers()==1&&!arrow.isAlive(),"Hrothgar spectral rower was not interruptible");for(int i=1;i<60;i++)rowers.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Hrothgar row surges struck before warning");for(int i=0;i<12;i++)rowers.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Hrothgar missing-row gap or staggered surge failed");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void realAttacks(GameTestHelper helper){
        var level=helper.getLevel(); var origin=helper.absolutePos(new net.minecraft.core.BlockPos(768,30,768));
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,6,15)))
            level.setBlock(pos,pos.getY()==origin.getY()-1 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var front=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var dodge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+3); dodge.snapTo(origin.getX(),origin.getY(),origin.getZ()-3);
        level.addFreshEntity(front); level.addFreshEntity(dodge);
        try {
        var boss=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND);
            boss.snapTo(origin.getX(),origin.getY(),origin.getZ()); boss.setNoAi(true);
            for(int i=0;i<40;i++) boss.tickCombat(level);
            helper.assertTrue(boss.beginAbility(BossCombatState.Ability.CLEAVE,front),"cleave rejected after initial cooldown");
            for(int i=0;i<29;i++) boss.tickCombat(level);
            helper.assertTrue(front.getHealth()==20,"cleave damaged during telegraph");
            boss.tickCombat(level);
            helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"cleave damage/cone counterplay failed");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); boss.saveWithoutId(output);
            var restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
            float after=front.getHealth(); for(int i=0;i<18;i++) restored.tickCombat(level);
            helper.assertTrue(front.getHealth()==after,"cleave impact replayed after disk reload");
            front.setHealth(20); front.invulnerableTime=0;
            boss=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND); boss.snapTo(origin.getX(),origin.getY(),origin.getZ());
            for(int i=0;i<40;i++) boss.tickCombat(level);
            helper.assertTrue(boss.beginAbility(BossCombatState.Ability.METEOR_RAIN,front),"meteor failed to begin");
            for(int i=0;i<20;i++) boss.tickCombat(level);
            output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); boss.saveWithoutId(output);
            restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
            dodge.snapTo(origin.getX()+10,origin.getY(),origin.getZ()-3);
            for(int i=0;i<29;i++) restored.tickCombat(level);
            helper.assertTrue(front.getHealth()==20,"meteor damaged before full warning after reload");
            restored.tickCombat(level);
            helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"meteor impact or marked-position dodge failed");
            front.setHealth(20); front.invulnerableTime=0;
            boss=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND); boss.snapTo(origin.getX(),origin.getY(),origin.getZ());
            for(int i=0;i<40;i++) boss.tickCombat(level);
            helper.assertTrue(boss.beginAbility(BossCombatState.Ability.ERUPTION,front),"eruption failed to begin");
            for(int i=0;i<45;i++) boss.tickCombat(level);
            helper.assertTrue(front.getHealth()==20,"eruption had no activation grace period");
            for(int i=0;i<20;i++) boss.tickCombat(level);
            helper.assertTrue(front.getHealth()<20 && level.getBlockState(front.blockPosition()).isAir(),"temporary zone failed or placed permanent lava");
            output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); boss.saveWithoutId(output);
            restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD); restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
            after=front.getHealth(); front.invulnerableTime=0; restored.tickCombat(level);
            helper.assertTrue(front.getHealth()==after,"hazard pulse repeated immediately after reload");
            front.setHealth(20); front.invulnerableTime=0;
            boss=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND); boss.snapTo(origin.getX(),origin.getY(),origin.getZ()); boss.setHealth(300);
            for(int i=0;i<115;i++) boss.tickCombat(level);
            helper.assertTrue(boss.phase()==2 && front.getHealth()==20,"phase transition missing or dealt unavoidable damage");
            helper.assertTrue(boss.beginAbility(BossCombatState.Ability.ERUPTION,front),"phase-two eruption could not start");
            for(int i=0;i<45;i++) boss.tickCombat(level);
            output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); boss.saveWithoutId(output);
            helper.assertTrue(output.buildResult().getIntOr("BossZoneTicks",0)==120,"phase-two danger duration did not increase");
            for(int i=0;i<20;i++) boss.tickCombat(level);
            helper.assertTrue(front.getHealth()<=13,"phase-two danger damage did not increase");
            frostboundAttacks(helper,boss,front,dodge,origin);
            helper.succeed();
        }finally { front.discard(); dodge.discard(); }
    }
    private static void frostboundAttacks(GameTestHelper helper,RegionalBossEntity boss,net.minecraft.world.entity.player.Player front,
            net.minecraft.world.entity.player.Player dodge,net.minecraft.core.BlockPos origin){
        var level=helper.getLevel();
        boss.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20); front.invulnerableTime=0;
        dodge.setHealth(20); dodge.invulnerableTime=0;
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        var frost=new FrostboundCombat(boss);
        helper.assertTrue(frost.begin(FrostboundCombat.FISSURE,front),"fissure failed to start");
        front.snapTo(origin.getX()+1.75,origin.getY(),origin.getZ()+3);
        dodge.snapTo(origin.getX()-8,origin.getY(),origin.getZ()-8);
        for(int i=0;i<39;i++) frost.tick(level);
        helper.assertTrue(front.getHealth()==20,"fissure hit before warning completed");
        frost.tick(level);
        helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"fissure damage or avoidance failed");
        front.setHealth(20); front.invulnerableTime=0;
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        frost=new FrostboundCombat(boss);
        helper.assertTrue(frost.begin(FrostboundCombat.WHEEL,front),"wheel failed to start");
        front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());
        dodge.snapTo(origin.getX()-4.24,origin.getY(),origin.getZ()+4.24);
        for(int i=0;i<50;i++) frost.tick(level);
        helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"wheel safe wedge or damage failed");
        front.setHealth(20); front.invulnerableTime=0;
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        frost=new FrostboundCombat(boss);
        helper.assertTrue(frost.begin(FrostboundCombat.LANCES,front),"lances failed to start");
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+4);
        dodge.snapTo(origin.getX()+3,origin.getY(),origin.getZ()+4);
        for(int i=0;i<60;i++) frost.tick(level);
        helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"first lance row damage failed");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); frost.save(output);
        var restored=new FrostboundCombat(boss);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        for(int i=0;i<19;i++) restored.tick(level);
        helper.assertTrue(dodge.getHealth()==20,"second lance row hit before warning");
        dodge.invulnerableTime=0;
        restored.tick(level);
        helper.assertTrue(dodge.getHealth()<20,"second lance row failed after reload");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); restored.save(output);
        frost=new FrostboundCombat(boss);
        frost.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        float after=dodge.getHealth(); dodge.invulnerableTime=0; frost.tick(level);
        helper.assertTrue(dodge.getHealth()==after,"second lance row repeated after reload");
        front.setHealth(20); front.invulnerableTime=0;
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()+3);
        var entity=BossRegistries.FROSTBOUND.get().create(level,EntitySpawnReason.COMMAND);
        entity.snapTo(origin.getX(),origin.getY(),origin.getZ()); entity.setTarget(front);
        for(int i=0;i<17;i++) entity.tickCombat(level);
        helper.assertTrue(front.getHealth()==20 && entity.frostAction()==1,"Frostbound AI failed to warn basic attack");
        entity.tickCombat(level);
        helper.assertTrue(front.getHealth()<20,"Frostbound AI did not resolve a real attack");
        entity.setHealth(entity.getMaxHealth()/2);
        for(int i=0;i<20;i++) entity.tickCombat(level);
        helper.assertTrue(entity.phase()==2 && entity.frostAction()==5,"Frostbound phase transition missing");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); entity.saveWithoutId(output);
        var reloaded=new FrostboundEntity(BossRegistries.MORVATH.get(),level);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        helper.assertTrue(reloaded.phase()==2 && reloaded.frostAction()==5,"Frostbound phase was lost on reload");
        for(int i=0;i<40;i++) reloaded.tickCombat(level);
        helper.assertTrue(reloaded.phase()==2 && reloaded.frostAction()!=5,"Frostbound transition failed to finish after reload");
        front.setHealth(20); front.invulnerableTime=0; front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        dodge.setHealth(20); dodge.invulnerableTime=0; dodge.snapTo(origin.getX()+3,origin.getY(),origin.getZ()+5);
        var hunt=new SkollvarCombat(boss);
        helper.assertTrue(hunt.begin(SkollvarCombat.CHARGE,front),"Skollvar scent charge failed to start");
        for(int i=0;i<39;i++) hunt.tick(level);
        helper.assertTrue(front.getHealth()==20,"Skollvar charge hit before the trail warning");
        hunt.tick(level);
        helper.assertTrue(front.getHealth()<20 && dodge.getHealth()==20,"Skollvar straight charge or sidestep counterplay failed");
        front.setHealth(20); front.invulnerableTime=0; front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        hunt=new SkollvarCombat(boss);
        helper.assertTrue(hunt.begin(SkollvarCombat.MIRAGES,front),"Skollvar pack mirages failed to start");
        front.snapTo(origin.getX()-3.5,origin.getY(),origin.getZ()+5);
        for(int i=0;i<49;i++) hunt.tick(level);
        helper.assertTrue(front.getHealth()==20,"Skollvar mirage struck before its landing warning");
        hunt.tick(level);
        helper.assertTrue(front.getHealth()<20,"Skollvar alternating pack pounce missed its marked shadow");
        front.setHealth(20); front.invulnerableTime=0; front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        hunt=new SkollvarCombat(boss);
        helper.assertTrue(hunt.begin(SkollvarCombat.PROWL,front),"Skollvar silent prowl failed to start");
        front.snapTo(origin.getX(),origin.getY(),origin.getZ()-5);
        for(int i=0;i<60;i++) hunt.tick(level);
        helper.assertTrue(front.getHealth()==20,"Skollvar prowl followed the moving player instead of the warned footprint");
        front.setHealth(20); front.invulnerableTime=0; dodge.setHealth(20); dodge.invulnerableTime=0;
        boss.setHealth(boss.getMaxHealth()/2);
        var feint=new SkollvarCombat(boss); var target=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        target.snapTo(origin.getX(),origin.getY(),origin.getZ()+5); level.addFreshEntity(target);
        helper.assertTrue(feint.updatePhase(300,600),"Skollvar phase two did not latch");
        for(int attack=0;attack<2;attack++){
            for(int i=0;i<80;i++) feint.tick(level);
            helper.assertTrue(feint.begin(SkollvarCombat.CHARGE,target),"Skollvar second-phase charge did not start");
            for(int i=0;i<70;i++) feint.tick(level);
        }
        target.setHealth(20); target.invulnerableTime=0;
        for(int i=0;i<80;i++) feint.tick(level);
        helper.assertTrue(feint.begin(SkollvarCombat.CHARGE,target),"Skollvar feint charge did not start");
        float before=target.getHealth(); for(int i=0;i<70;i++) feint.tick(level);
        helper.assertTrue(target.getHealth()==before,"Skollvar feinted third charge dealt damage");
        target.discard();

        var treant=BossRegistries.ELDAROTH.get().create(level,EntitySpawnReason.COMMAND);treant.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+5,origin.getY(),origin.getZ());
        dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var roots=new EldarothCombat(treant);helper.assertTrue(roots.begin(EldarothCombat.ROOTS,front),"Eldaroth root lattice failed to start");
        for(int i=0;i<39;i++)roots.tick(level);helper.assertTrue(front.getHealth()==20,"Eldaroth roots hit before full warning");roots.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Eldaroth root lattice damage or safe-cell counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6.5,origin.getY(),origin.getZ());
        var pods=new EldarothCombat(treant);helper.assertTrue(pods.begin(EldarothCombat.PODS,front),"Eldaroth seed pods failed to start");front.snapTo(origin.getX(),origin.getY(),origin.getZ());
        for(int i=0;i<49;i++)pods.tick(level);helper.assertTrue(front.getHealth()==20,"Eldaroth seed pods hit before landing");pods.tick(level);
        helper.assertTrue(front.getHealth()==20,"Eldaroth pod landing did not preserve fixed-mark dodge");
        front.snapTo(origin.getX()+6.5,origin.getY(),origin.getZ());front.invulnerableTime=0;pods=new EldarothCombat(treant);pods.begin(EldarothCombat.PODS,front);
        for(int i=0;i<50;i++)pods.tick(level);helper.assertTrue(front.getHealth()<20,"Eldaroth seed pod failed to damage at its telegraphed landing");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var branch=new EldarothCombat(treant);helper.assertTrue(branch.begin(EldarothCombat.BRANCH,front),"Eldaroth branch clock failed to start");
        for(int i=0;i<59;i++)branch.tick(level);helper.assertTrue(front.getHealth()==20,"Eldaroth branch hit before sweep warning");branch.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Eldaroth branch sweep or inner refuge counterplay failed");
        treant.setHealth(treant.getMaxHealth()/2);treant.tickCombat(level);
        helper.assertTrue(treant.phase()==2,"Eldaroth phase two failed to latch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());treant.saveWithoutId(output);
        var eldarothReloaded=BossRegistries.ELDAROTH.get().create(level,EntitySpawnReason.LOAD);eldarothReloaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        helper.assertTrue(eldarothReloaded.phase()==2,"Eldaroth phase was lost on save/reload");
        var doubleBloom=new EldarothCombat(treant);helper.assertTrue(doubleBloom.updatePhase(450,900)&&doubleBloom.begin(EldarothCombat.PODS,front),"Eldaroth phase-two seed bloom failed to start");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+5.6,origin.getY(),origin.getZ());
        for(int i=0;i<50;i++)doubleBloom.tick(level);helper.assertTrue(front.getHealth()<20,"Eldaroth phase-two first bloom did not hit its warning mark");
        front.setHealth(20);front.invulnerableTime=0;double nextX=origin.getX()+Math.cos(0.43)*5.6,nextZ=origin.getZ()+Math.sin(0.43)*5.6;front.snapTo(nextX,origin.getY(),nextZ);
        for(int i=0;i<15;i++)doubleBloom.tick(level);helper.assertTrue(front.getHealth()==20,"Eldaroth second bloom fired before its warning interval");doubleBloom.tick(level);
        helper.assertTrue(front.getHealth()<20,"Eldaroth second phase-two bloom did not resolve after its second warning");

        var king=BossRegistries.FALLEN_KING.get().create(level,EntitySpawnReason.COMMAND);king.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;
        var duel=new FallenKingCombat(king);helper.assertTrue(duel.begin(FallenKingCombat.DUEL,front),"Fallen King royal duel failed to start");front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());
        for(int i=0;i<39;i++)duel.tick(level);helper.assertTrue(front.getHealth()==20,"Fallen King thrust hit before its warning");duel.tick(level);
        helper.assertTrue(front.getHealth()==20,"Fallen King thrust followed the player instead of the announced lane");
        front.snapTo(origin.getX()+5,origin.getY(),origin.getZ());front.invulnerableTime=0;duel=new FallenKingCombat(king);duel.begin(FallenKingCombat.DUEL,front);
        for(int i=0;i<40;i++)duel.tick(level);helper.assertTrue(front.getHealth()<20,"Fallen King announced thrust missed its marked lane");
        front.setHealth(20);front.invulnerableTime=0;duel=new FallenKingCombat(king);helper.assertTrue(duel.updatePhase(350,700)&&duel.begin(FallenKingCombat.DUEL,front),"Fallen King phase-two feint failed to start");
        for(int i=0;i<40;i++)duel.tick(level);helper.assertTrue(front.getHealth()==20,"Fallen King feint hit before its riposte");
        for(int i=0;i<17;i++)duel.tick(level);helper.assertTrue(front.getHealth()==20,"Fallen King riposte ignored its second warning");duel.tick(level);
        helper.assertTrue(front.getHealth()<20,"Fallen King phase-two riposte failed to hit after warning");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+7,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var cavalry=new FallenKingCombat(king);helper.assertTrue(cavalry.begin(FallenKingCombat.CAVALRY,front),"Fallen King cavalry failed to start");front.snapTo(origin.getX()+7,origin.getY(),origin.getZ()+6);
        for(int i=0;i<49;i++)cavalry.tick(level);helper.assertTrue(dodge.getHealth()==20,"Fallen King cavalry struck before lane warning");cavalry.tick(level);
        helper.assertTrue(dodge.getHealth()<20&&front.getHealth()==20,"Fallen King heralded cavalry lanes or missing-lane counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ()+6);
        var regalia=new FallenKingCombat(king);helper.assertTrue(regalia.begin(FallenKingCombat.REGALIA,front),"Fallen King broken regalia failed to start");
        for(int i=0;i<59;i++)regalia.tick(level);helper.assertTrue(dodge.getHealth()==20,"Fallen King regalia hit before crest warning");regalia.tick(level);
        helper.assertTrue(dodge.getHealth()<20&&front.getHealth()==20,"Fallen King regalia safe exposed sector failed");

        var warlord=BossRegistries.KRAGMOR.get().create(level,EntitySpawnReason.COMMAND);warlord.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+4,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7.3,origin.getY(),origin.getZ());
        var rockfall=new KragmorCombat(warlord);helper.assertTrue(rockfall.begin(KragmorCombat.ROCKFALL,front),"Kragmor rockfall failed to start");
        for(int i=0;i<39;i++)rockfall.tick(level);helper.assertTrue(front.getHealth()==20,"Kragmor rockfall struck before shadow warning");rockfall.tick(level);
        for(int i=0;i<7;i++)rockfall.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Kragmor first rock landed before its shadow interval");rockfall.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Kragmor first locked rockfall missed its shadow");
        for(int i=0;i<7;i++)rockfall.tick(level);helper.assertTrue(dodge.getHealth()==20,"Kragmor second rock landed before its new warning interval");rockfall.tick(level);
        helper.assertTrue(dodge.getHealth()<20,"Kragmor sequential second locked rock missed its shadow");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+3,origin.getY(),origin.getZ()-3);dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var tethers=new KragmorCombat(warlord);helper.assertTrue(tethers.begin(KragmorCombat.TETHERS,front),"Kragmor cairn tethers failed to start");
        for(int i=0;i<49;i++)tethers.tick(level);helper.assertTrue(front.getHealth()==20,"Kragmor tethers struck before web warning");tethers.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Kragmor tether segment damage or outside-web counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+13,origin.getY(),origin.getZ());
        var leap=new KragmorCombat(warlord);helper.assertTrue(leap.begin(KragmorCombat.LEAP,front),"Kragmor ravine leap failed to start");
        for(int i=0;i<59;i++)leap.tick(level);helper.assertTrue(front.getHealth()==20,"Kragmor landing circle struck before warning");leap.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Kragmor leap landing circle or locked destination failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ()+5);dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+13,origin.getY(),origin.getZ());
        for(int i=0;i<17;i++)leap.tick(level);helper.assertTrue(dodge.getHealth()==20,"Kragmor shock line hit before second warning");leap.tick(level);
        helper.assertTrue(dodge.getHealth()<20&&front.getHealth()==20,"Kragmor narrow follow-up shock line or sidestep counterplay failed");
        warlord.setHealth(warlord.getMaxHealth()/2);warlord.tickCombat(level);helper.assertTrue(warlord.phase()==2,"Kragmor phase two did not latch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());warlord.saveWithoutId(output);var warlordReload=BossRegistries.KRAGMOR.get().create(level,EntitySpawnReason.LOAD);
        warlordReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));helper.assertTrue(warlordReload.phase()==2,"Kragmor phase was lost on reload");

        var nocthar=BossRegistries.NOCTHAR.get().create(level,EntitySpawnReason.COMMAND);nocthar.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;
        var seams=new NoctharCombat(nocthar);helper.assertTrue(seams.begin(NoctharCombat.SEAMS,front),"Nocthar void seam failed to start");
        front.snapTo(origin.getX()+3.535,origin.getY(),origin.getZ()+3.535);dodge.snapTo(origin.getX()-3.535,origin.getY(),origin.getZ()+3.535);
        for(int i=0;i<39;i++)seams.tick(level);helper.assertTrue(front.getHealth()==20,"Nocthar seam ruptured before the crossing warning");seams.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Nocthar paired seam crossing or unlinked safe route failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+6,origin.getY(),origin.getZ()+9);front.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        var wells=new NoctharCombat(nocthar);helper.assertTrue(wells.begin(NoctharCombat.WELLS,front),"Nocthar gravity wells failed to start");front.snapTo(origin.getX()+6.8,origin.getY(),origin.getZ()+3.5);
        for(int i=0;i<49;i++)wells.tick(level);helper.assertTrue(front.getHealth()==20,"Nocthar gravity well damaged before charge");wells.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&front.getDeltaMovement().horizontalDistanceSqr()>0,"Nocthar bounded gravity pull/damage or exit-boundary counterplay failed: front="+front.getHealth()+", dodge="+dodge.getHealth()+", velocity="+front.getDeltaMovement()+", position="+front.position());
        front.setHealth(20);front.invulnerableTime=0;dodge.setHealth(20);dodge.invulnerableTime=0;
        var eclipse=new NoctharCombat(nocthar);helper.assertTrue(eclipse.begin(NoctharCombat.ECLIPSE,front),"Nocthar eclipse lanes failed to start");
        double beam=Math.PI/8,safe=Math.PI*3/8;front.snapTo(origin.getX()+Math.cos(beam)*6,origin.getY(),origin.getZ()+Math.sin(beam)*6);dodge.snapTo(origin.getX()+Math.cos(safe)*6,origin.getY(),origin.getZ()+Math.sin(safe)*6);
        for(int i=0;i<59;i++)eclipse.tick(level);helper.assertTrue(front.getHealth()==20,"Nocthar eclipse beam fired before lane warning");eclipse.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Nocthar rotating beam lane or unshaded corridor failed");
        nocthar.setHealth(nocthar.getMaxHealth()/2);nocthar.tickCombat(level);helper.assertTrue(nocthar.phase()==2,"Nocthar phase two did not latch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());nocthar.saveWithoutId(output);var noctharReload=BossRegistries.NOCTHAR.get().create(level,EntitySpawnReason.LOAD);
        noctharReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));helper.assertTrue(noctharReload.phase()==2,"Nocthar phase or spatial state was lost on reload");

        var aetherion=BossRegistries.AETHERION.get().create(level,EntitySpawnReason.COMMAND);aetherion.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+7,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var updraft=new AetherionCombat(aetherion);helper.assertTrue(updraft.begin(AetherionCombat.UPDRAFT,front),"Aetherion updraft path failed to start");front.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        for(int i=0;i<39;i++)updraft.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Aetherion sweep struck before its warning");updraft.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&dodge.getDeltaMovement().y>0,"Aetherion low sweep or lift counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+5.5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()-2.75,origin.getY(),origin.getZ()-2.75);
        var feathers=new AetherionCombat(aetherion);helper.assertTrue(feathers.begin(AetherionCombat.FEATHERS,front),"Aetherion feather constellation failed to start");
        for(int i=0;i<49;i++)feathers.tick(level);helper.assertTrue(dodge.getHealth()==20,"Aetherion feathers struck before their link warning");feathers.tick(level);
        helper.assertTrue(front.getHealth()==20&&dodge.getHealth()<20,"Aetherion interruptible node or connected feather lane failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+2,origin.getY(),origin.getZ());
        var dive=new AetherionCombat(aetherion);helper.assertTrue(dive.begin(AetherionCombat.DIVE,front),"Aetherion sky dive failed to start");
        for(int i=0;i<59;i++)dive.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Aetherion dive landed before its locked shadow warning");dive.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Aetherion marked dive circle or sidestep failed");
        front.snapTo(origin.getX()+14,origin.getY(),origin.getZ());for(int i=0;i<17;i++)dive.tick(level);
        helper.assertTrue(dodge.getHealth()==20,"Aetherion wake ignored its second warning");dive.tick(level);
        helper.assertTrue(dodge.getHealth()<20,"Aetherion trailing wake failed to resolve at the marked lane");
        aetherion.setHealth(aetherion.getMaxHealth()/2);aetherion.tickCombat(level);helper.assertTrue(aetherion.phase()==2,"Aetherion phase two failed to latch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());aetherion.saveWithoutId(output);
        var aetherionReload=BossRegistries.AETHERION.get().create(level,EntitySpawnReason.LOAD);aetherionReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        helper.assertTrue(aetherionReload.phase()==2&&aetherionReload.isNoGravity()&&aetherionReload.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation,
            "Aetherion phase/3D navigation did not survive entity reload");

        var monarch=BossRegistries.VERMILLION.get().create(level,EntitySpawnReason.COMMAND);monarch.snapTo(origin.getX(),origin.getY(),origin.getZ());
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var bloodTethers=new VermillionCombat(monarch);helper.assertTrue(bloodTethers.begin(VermillionCombat.TETHERS,front),"Vermillion blood vessels failed to start");front.snapTo(origin.getX()+3,origin.getY(),origin.getZ()+2.6);refreshMockPositions(level,front,dodge);
        for(int i=0;i<39;i++)bloodTethers.tick(level);helper.assertTrue(front.getHealth()==20,"Vermillion tether drained before warning");bloodTethers.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Vermillion line damage or vessel interruption failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6.5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var orbits=new VermillionCombat(monarch);helper.assertTrue(orbits.begin(VermillionCombat.ORBITS,front),"Vermillion blade orbit failed to start");
        for(int i=0;i<49;i++)orbits.tick(level);helper.assertTrue(front.getHealth()==20,"Vermillion orbit struck before blade arrival");orbits.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Vermillion staggered blade lane or center refuge failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ()+5.8);dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+5.5,origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var court=new VermillionCombat(monarch);helper.assertTrue(court.begin(VermillionCombat.COURT,front),"Vermillion court channel failed to start");
        for(int i=0;i<59;i++)court.tick(level);helper.assertTrue(front.getHealth()==20,"Vermillion channel resolved before warning");court.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Vermillion limited channel sector or courtier interruption failed: front="+front.getHealth()+", dodge="+dodge.getHealth()+", active="+court.clock().active()+", resolved="+court.clock().resolved()+", insideFront="+monarch.insideArena(front.position())+", front="+front.position()+", dodge="+dodge.position()+", nearby="+level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,monarch.getBoundingBox().inflate(28)).size()+", boss="+monarch.position());
        var breaker=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(breaker);monarch.setHealth(700);
        front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());var phaseTwo=new VermillionCombat(monarch);helper.assertTrue(phaseTwo.updatePhase(700,1400)&&phaseTwo.begin(VermillionCombat.TETHERS,front),"Vermillion phase-two tethers failed");
        front.snapTo(origin.getX()+5.36,origin.getY(),origin.getZ()-4.62);dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);breaker.snapTo(origin.getX()+5.36,origin.getY(),origin.getZ()+4.62);
        for(int i=0;i<40;i++)phaseTwo.tick(level);helper.assertTrue(phaseTwo.exposed(),"breaking all phase-two vessels did not expose the monarch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());phaseTwo.save(output);var phaseTwoReload=new VermillionCombat(monarch);
        phaseTwoReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        helper.assertTrue(phaseTwoReload.exposed()&&phaseTwoReload.clock().phase()==2,"Vermillion exposed vulnerability or phase was lost on reload");breaker.discard();

        var jadeDragon=BossRegistries.LONGWEI.get().create(level,EntitySpawnReason.COMMAND);jadeDragon.snapTo(origin.getX(),origin.getY(),origin.getZ());jadeDragon.setNoAi(true);level.addFreshEntity(jadeDragon);
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+7,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ()+4);
        var jadeBreath=new LongweiCombat(jadeDragon);helper.assertTrue(jadeBreath.begin(LongweiCombat.RIBBON,front),"Longwei serpentine jade ribbon failed to start");
        for(int i=0;i<39;i++)jadeBreath.tick(level);helper.assertTrue(front.getHealth()==20,"Longwei breath ignited before route lock warning");jadeBreath.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Longwei locked ribbon path or cross-behind counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+2.5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var spiral=new LongweiCombat(jadeDragon);helper.assertTrue(spiral.begin(LongweiCombat.SPIRAL,front),"Longwei spiral dive failed to start");
        for(int i=0;i<49;i++)spiral.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Longwei spiral struck before its dive warning");spiral.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Longwei spiral route or central refuge failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6.5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());refreshMockPositions(level,front,dodge);
        var orbs=new LongweiCombat(jadeDragon);helper.assertTrue(orbs.begin(LongweiCombat.ORBITS,front),"Longwei five-orb sequence failed to start");var firstJadeOrb=orbs.orbPosition(0);front.snapTo(firstJadeOrb.x,firstJadeOrb.y,firstJadeOrb.z);front.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);dodge.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        for(int i=0;i<59;i++)orbs.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Longwei orbit pulse fired before full warning");orbs.tick(level);
        helper.assertTrue(front.getHealth()==20,"Longwei first orb pulse ignored its individual color telegraph");orbs.tick(level);orbs.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Longwei sequential orb pulse or between-orbs counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var jadeCombo=new LongweiCombat(jadeDragon);
        helper.assertTrue(jadeCombo.updatePhase(550,1100)&&jadeCombo.begin(LongweiCombat.RIBBON,front),"Longwei phase-two breath failed to start");
        for(int i=0;i<40;i++)jadeCombo.tick(level);front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+2.5,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());jadeCombo.save(output);var jadeComboReload=new LongweiCombat(jadeDragon);
        jadeComboReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        for(int i=0;i<15;i++)jadeComboReload.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Longwei phase-two spiral follow-up resolved early or was lost on reload");jadeComboReload.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&jadeComboReload.clock().phase()==2,"Longwei saved separate spiral follow-up or central refuge failed");

        var queen=BossRegistries.MORGATHA.get().create(level,EntitySpawnReason.COMMAND);queen.snapTo(origin.getX(),origin.getY(),origin.getZ());queen.setNoAi(true);level.addFreshEntity(queen);
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()-6,origin.getY(),origin.getZ());
        var bubbles=new MorgathaCombat(queen);helper.assertTrue(bubbles.begin(MorgathaCombat.BUBBLES,front),"Morgatha swelling bog bubbles failed to start");
        for(int i=0;i<39;i++)bubbles.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Morgatha bubbles hit before warning finished");bubbles.tick(level);
        var bubbleSave=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());bubbles.save(bubbleSave);var bubblesReload=new MorgathaCombat(queen);bubblesReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(bubbleSave.buildResult())));
        for(int i=0;i<19;i++)bubblesReload.tick(level);helper.assertTrue(front.getHealth()==20,"Morgatha poison pool pulsed before its safe window");bubblesReload.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&front.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Morgatha pool damage/poison or leave-the-circle counterplay failed");
        front.setHealth(20);front.removeEffect(net.minecraft.world.effect.MobEffects.POISON);front.invulnerableTime=0;front.snapTo(origin.getX()+2,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+8,origin.getY(),origin.getZ());
        var veil=new MorgathaCombat(queen);helper.assertTrue(veil.begin(MorgathaCombat.VEIL,front),"Morgatha marked mosquito veil failed to start");for(int i=0;i<50;i++)veil.tick(level);
        for(int i=0;i<16;i++)veil.tick(level);helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Morgatha slow target cloud failed to track one marked player");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var hex=new MorgathaCombat(queen);helper.assertTrue(hex.begin(MorgathaCombat.DECOYS,front),"Morgatha charged decoy hex failed to start");for(int i=0;i<59;i++)hex.tick(level);
        helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Morgatha decoy wave resolved before charge completed");hex.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Morgatha decoy direction or safe-lane counterplay failed");
        front.setHealth(20);front.removeEffect(net.minecraft.world.effect.MobEffects.POISON);front.invulnerableTime=0;front.snapTo(origin.getX()+20,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var blightedHex=new MorgathaCombat(queen);helper.assertTrue(blightedHex.updatePhase(400,800)&&blightedHex.begin(MorgathaCombat.DECOYS,front),"Morgatha phase-two charged decoy failed to start");
        for(int i=0;i<60;i++)blightedHex.tick(level);for(int i=0;i<8;i++)blightedHex.tick(level);output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());blightedHex.save(output);var blightedReload=new MorgathaCombat(queen);blightedReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        var markedWave=blightedReload.phaseWaveTarget();front.snapTo(markedWave.x,markedWave.y,markedWave.z);
        for(int i=0;i<5;i++)blightedReload.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20&&blightedReload.phaseWaveDelay()==1,"Morgatha phase-two warning/save state resolved early or lost: delay="+blightedReload.phaseWaveDelay());blightedReload.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&blightedReload.clock().phase()==2,"Morgatha extended phase-two warning or save state failed: front="+front.getHealth()+", dodge="+dodge.getHealth()+", phase="+blightedReload.clock().phase()+", delay="+blightedReload.phaseWaveDelay());

        var emperor=BossRegistries.AURELIOS.get().create(level,EntitySpawnReason.COMMAND);emperor.snapTo(origin.getX(),origin.getY(),origin.getZ());emperor.setNoAi(true);level.addFreshEntity(emperor);
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()-8,origin.getY(),origin.getZ());
        var mirrors=new AureliosCombat(emperor);helper.assertTrue(mirrors.begin(AureliosCombat.MIRRORS,front),"Aurelios water mirrors failed to begin");for(int i=0;i<39;i++)mirrors.tick(level);
        helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Aurelios mirror beams struck before warning");mirrors.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Aurelios mirrored ray sectors or open flank failed");
        var guarded=emperor.damageSources().mobAttack(front);helper.assertTrue(mirrors.damageMultiplier(guarded)<0.3f,"Aurelios mirror direction did not shield incoming damage");
        helper.assertTrue(mirrors.damageMultiplier(emperor.damageSources().mobAttack(dodge))==1f,"Aurelios open flank was incorrectly shielded");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+3.2,origin.getY()+0.8,origin.getZ());helper.assertTrue(mirrors.damageMultiplier(emperor.damageSources().mobAttack(front))==1f,"Aurelios exposed mirror core did not open the damage lane");
        front.snapTo(origin.getX()-4,origin.getY(),origin.getZ()+6.93);for(int i=0;i<31;i++)mirrors.tick(level);
        helper.assertTrue(mirrors.clock().active().isEmpty()&&mirrors.damageMultiplier(emperor.damageSources().mobAttack(front))<0.3f,"Aurelios directional shield ended with its attack recovery instead of its saved duration");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var grid=new AureliosCombat(emperor);helper.assertTrue(grid.begin(AureliosCombat.GRID,front),"Aurelios pressure grid failed to begin");front.snapTo(origin.getX()-6.3,origin.getY(),origin.getZ());
        for(int i=0;i<49;i++)grid.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Aurelios alternating pressure row fired early");grid.tick(level);grid.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Aurelios active pressure row/inactive row counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ()+5);
        for(int i=0;i<79;i++)mirrors.tick(level);var tithe=mirrors;helper.assertTrue(tithe.begin(AureliosCombat.TITHE,front),"Aurelios treasury tribute failed to begin");for(int i=0;i<60;i++)tithe.tick(level);
        int shieldBeforeOrb=tithe.shieldTicks();var tribute=tithe.tributeOrbPosition(0);front.snapTo(tribute.x,tribute.y,tribute.z);tithe.tick(level);helper.assertTrue(tithe.interceptedOrbs()==1&&tithe.shieldTicks()<shieldBeforeOrb,"Aurelios tribute orb did not intercept safely or shorten the mirror shield");
        for(int i=0;i<31;i++)tithe.tick(level);helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Aurelios intercepted-orb path or announced pulse counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var phaseTwoMirrors=new AureliosCombat(emperor);
        helper.assertTrue(phaseTwoMirrors.updatePhase(475,950)&&phaseTwoMirrors.begin(AureliosCombat.MIRRORS,front),"Aurelios phase-two mirrors did not begin");for(int i=0;i<40;i++)phaseTwoMirrors.tick(level);
        helper.assertTrue(phaseTwoMirrors.clock().phase()==2&&phaseTwoMirrors.shieldTicks()==180,"Aurelios phase-two shorter mirror shield failed");

        var serpent=BossRegistries.LIANHUA.get().create(level,EntitySpawnReason.COMMAND);serpent.snapTo(origin.getX(),origin.getY(),origin.getZ());serpent.setNoAi(true);level.addFreshEntity(serpent);
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var petals=new LianhuaCombat(serpent);helper.assertTrue(petals.begin(LianhuaCombat.PETALS,front),"Lianhua lotus-petal arcs failed to begin");for(int i=0;i<39;i++)petals.tick(level);
        helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Lianhua petal arc resolved before warning");petals.tick(level);petals.tick(level);petals.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Lianhua petal danger/safe lily counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+10,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var coil=new LianhuaCombat(serpent);helper.assertTrue(coil.begin(LianhuaCombat.COILS,front),"Lianhua river coil failed to begin");for(int i=0;i<49;i++)coil.tick(level);
        helper.assertTrue(front.getHealth()==20&&dodge.getHealth()==20,"Lianhua previewed coil struck early");coil.tick(level);helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Lianhua coil route/opening counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+6.9,origin.getY(),origin.getZ()+1.25);dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX(),origin.getY(),origin.getZ());
        var bloom=new LianhuaCombat(serpent);helper.assertTrue(bloom.begin(LianhuaCombat.BLOOMS,front),"Lianhua reflected bloom failed to begin");for(int i=0;i<60;i++)bloom.tick(level);
        helper.assertTrue(front.getHealth()<20,"Lianhua first bloom pulse failed");front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()-6.9,origin.getY(),origin.getZ()-1.25);
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());bloom.save(output);var bloomReload=new LianhuaCombat(serpent);bloomReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));
        for(int i=0;i<17;i++)bloomReload.tick(level);helper.assertTrue(front.getHealth()==20,"Lianhua reflected bloom echoed without its separate warning");bloomReload.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Lianhua delayed reflected bloom or save state failed");
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+8,origin.getY(),origin.getZ());var phaseTwoPetals=new LianhuaCombat(serpent);
        helper.assertTrue(phaseTwoPetals.updatePhase(425,850)&&phaseTwoPetals.begin(LianhuaCombat.PETALS,front),"Lianhua phase-two petal attack failed to begin");for(int i=0;i<42;i++)phaseTwoPetals.tick(level);
        front.snapTo(origin.getX()-8,origin.getY(),origin.getZ());helper.assertTrue(phaseTwoPetals.tailExposure()>0&&phaseTwoPetals.damageMultiplier(serpent.damageSources().mobAttack(front))>1.4f,"Lianhua phase-two rotating gaps or exposed tail vulnerability failed");


    }
    private static BossCombatState ready(){ var state=new BossCombatState(); for(int i=0;i<40;i++) state.tick(); return state; }
    private static BossCombatState reload(BossCombatState original){
        var copy=new BossCombatState(); copy.restore(original.phase(),original.ability().ordinal(),original.remaining(),original.cooldown(),original.resolved()); return copy;
    }
    private static void combatReload(GameTestHelper helper){
        for(var ability:List.of(BossCombatState.Ability.CLEAVE,BossCombatState.Ability.METEOR_RAIN,BossCombatState.Ability.ERUPTION)){
            var state=ready(); helper.assertTrue(state.begin(ability),"ability failed to start");
            helper.assertTrue(!state.begin(ability),"overlapping ability accepted");
            for(int i=1;i<ability.windup;i++){
                helper.assertTrue(state.tick()==BossCombatState.Ability.NONE,"impact before complete windup");
                state=reload(state);
            }
            helper.assertTrue(state.tick()==ability,"impact missing after windup and repeated reload");
            state=reload(state);
            for(int i=0;i<ability.recovery+55;i++) helper.assertTrue(state.tick()==BossCombatState.Ability.NONE,"saved impact applied twice");
            helper.assertTrue(state.ready(),"recovery/cooldown never completed");
        }
        var timings=Map.of("fissure",new RegionalCombatClock.Timing(40,30,80),"wheel",new RegionalCombatClock.Timing(50,40,100));
        var regional=new RegionalCombatClock(timings);
        helper.assertTrue(regional.begin("fissure") && !regional.begin("wheel"),"regional clock accepted overlapping attacks");
        for(int i=0;i<39;i++) helper.assertTrue(regional.tick().isEmpty(),"regional impact before windup");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,helper.getLevel().registryAccess()); regional.save(output);
        var restored=new RegionalCombatClock(timings); restored.load(TagValueInput.create(ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),diskRoundtrip(output.buildResult())));
        helper.assertTrue(restored.tick().equals("fissure"),"regional impact lost on reload");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,helper.getLevel().registryAccess()); restored.save(output);
        regional=new RegionalCombatClock(timings); regional.load(TagValueInput.create(ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),diskRoundtrip(output.buildResult())));
        for(int i=0;i<30;i++) helper.assertTrue(regional.tick().isEmpty(),"regional impact repeated after reload");
        helper.assertTrue(regional.cooldown()==80 && !regional.begin("wheel"),"regional cooldown was not applied");
        helper.assertTrue(regional.updatePhase(50,100) && !regional.updatePhase(40,100) && regional.phase()==2,"regional phase transition not latched");
        helper.succeed();
    }
    private static void phaseLatch(GameTestHelper helper){
        var state=ready(); state.begin(BossCombatState.Ability.CLEAVE); state.updatePhase(300,600);
        helper.assertTrue(state.phase()==2 && state.ability()==BossCombatState.Ability.PHASE_CHANGE,"half-health transition missing");
        state=reload(state); state.updatePhase(600,600);
        helper.assertTrue(state.phase()==2,"healing or reload reset second phase");
        int transitions=0;
        for(int i=0;i<200;i++){ state.updatePhase(100,600); if(state.tick()==BossCombatState.Ability.PHASE_CHANGE) transitions++; }
        helper.assertTrue(transitions==1 && state.ready(),"phase transition repeated or permanently blocked combat");
        state.restore(99,999,-100,Integer.MAX_VALUE,false);
        helper.assertTrue(state.phase()==1 && state.ability()==BossCombatState.Ability.NONE && state.cooldown()==1200,"invalid save fields were not bounded");
        helper.succeed();
    }
    private static void unloadAndCleanup(GameTestHelper helper){
        var level=helper.getLevel(); var data=BossEncounters.get(level);
        var arena=helper.absolutePos(new net.minecraft.core.BlockPos(512,30,512));
        var entity=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND);
        entity.snapTo(arena.getX(),arena.getY(),arena.getZ());
        helper.assertTrue(data.reserve(entity.getUUID(),UUID.randomUUID(),BossCatalog.MORVATH.id(),arena),"cleanup fixture reservation failed");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); entity.saveWithoutId(output);
        var tag=output.buildResult(); tag.putBoolean("ManagedEncounter",true); tag.putInt("BossAbandonedTicks",599);
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
        entity.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(data.find(entity.getUUID()).orElseThrow().status()==BossEncounters.Status.ACTIVE,"chunk unload cancelled persistent encounter");
        var restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(tag)));
        restored.setNoAi(true); restored.tick();
        helper.assertTrue(restored.isRemoved() && data.find(restored.getUUID()).orElseThrow().status()==BossEncounters.Status.CANCELLED,"abandoned encounter with NoAI was not cleaned up after reload");
        var discarded=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND);
        helper.assertTrue(data.reserve(discarded.getUUID(),UUID.randomUUID(),BossCatalog.MORVATH.id(),arena),"terminal arena remained reserved");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); discarded.saveWithoutId(output);
        tag=output.buildResult(); tag.putBoolean("ManagedEncounter",true);
        discarded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag)); discarded.discard();
        helper.assertTrue(data.find(discarded.getUUID()).orElseThrow().status()==BossEncounters.Status.CANCELLED,"permanent entity removal leaked reservation");
        helper.succeed();
    }
    private static void summoningLifecycle(GameTestHelper helper){
        var level=helper.getLevel(); var arena=helper.absolutePos(new net.minecraft.core.BlockPos(256,25,256));
        for(int x=-32;x<=32;x+=8) for(int z=-32;z<=32;z+=8) level.getChunkAt(arena.offset(x,0,z));
        var chunk=net.minecraft.world.level.ChunkPos.containing(arena);
        level.getChunkSource().addTicketAndLoadWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunk,2);
        var owner=helper.makeMockServerPlayerInLevel(); owner.snapTo(arena.getX()+6,arena.getY(),arena.getZ());
        try {
            helper.assertTrue(BossEncounterService.summonMorvath(owner,arena)==BossEncounterService.Result.WRONG_BIOME,"non-Ashlands ritual accepted");
            var ashlands=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("ashlands/ash_waste_plains")));
            var filled=net.minecraft.server.commands.FillBiomeCommand.fill(level,arena.offset(-8,-8,-8),arena.offset(8,8,8),ashlands);
            helper.assertTrue(filled.left().isPresent(),"fixture biome fill failed");
            helper.assertTrue(BossEncounterService.morvathBiome(level,arena) && !BossEncounterService.morvathBiome(level,arena.above(40)),"boss biome check ignored Y");
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(arena.offset(-6,-1,-6),arena.offset(6,5,6)))
                level.setBlock(pos,pos.getY()==arena.getY()-1 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            var obstruction=arena.above(2); level.setBlock(obstruction,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),2);
            helper.assertTrue(BossEncounterService.summonMorvath(owner,arena)==BossEncounterService.Result.OBSTRUCTED,"obstructed player arena accepted");
            helper.assertTrue(level.getBlockState(obstruction).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),"summoning modified player blocks");
            level.setBlock(obstruction,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            helper.assertTrue(BossEncounterService.summonMorvath(owner,arena)==BossEncounterService.Result.SUCCESS,"valid player-built Ashlands arena rejected");
            var boss=level.getEntitiesOfClass(RegionalBossEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).getFirst();
            helper.assertTrue(BossEncounterService.summonMorvath(owner,arena)==BossEncounterService.Result.NEARBY_ENCOUNTER,"duplicate encounter accepted");
            helper.assertTrue(!BossEncounterService.cancel(level,boss.getUUID(),UUID.randomUUID()),"non-owner cancelled encounter");
            helper.assertTrue(BossEncounterService.cancel(level,boss.getUUID(),owner.getUUID()),"owner cancellation rejected");
            helper.runAfterDelay(20,()->{
                try {
                    helper.assertTrue(boss.isRemoved(),"cancelled encounter entity remained live (entity ticks="+boss.tickCount+")");
                    helper.assertTrue(!BossEncounters.get(level).conflicts(arena),"cancelled encounter retained arena lock");
                    helper.assertTrue(level.getBlockState(arena.below()).is(net.minecraft.world.level.block.Blocks.STONE),"encounter altered player floor");
                    helper.succeed();
                }finally{ level.getServer().getPlayerList().remove(owner); level.getChunkSource().removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunk,2); }
            });
        }catch(RuntimeException|Error failure){ level.getServer().getPlayerList().remove(owner); level.getChunkSource().removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunk,2); throw failure; }
    }
    private static void encounterReservations(GameTestHelper helper){
        var encounters=new BossEncounters(); var boss=UUID.randomUUID(); var owner=UUID.randomUUID();
        var arena=new net.minecraft.core.BlockPos(100,70,100);
        helper.assertTrue(encounters.reserve(boss,owner,"morvath_ashen_king",arena),"first reservation rejected");
        helper.assertTrue(!encounters.reserve(UUID.randomUUID(),UUID.randomUUID(),"other",arena.offset(40,0,0)),"nearby different boss overlapped arena");
        var saved=BossEncounters.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,encounters).getOrThrow();
        var envelope=new net.minecraft.nbt.CompoundTag(); envelope.put("entries",saved);
        encounters=BossEncounters.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,diskRoundtrip(envelope).get("entries")).getOrThrow();
        helper.assertTrue(encounters.conflicts(arena) && encounters.find(boss).orElseThrow().owner().equals(owner),"unloaded reservation was lost");
        helper.assertTrue(encounters.finish(boss,BossEncounters.Status.DEFEATED),"first completion rejected");
        var completed=new net.minecraft.nbt.CompoundTag(); completed.put("entries",BossEncounters.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,encounters).getOrThrow());
        encounters=BossEncounters.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,diskRoundtrip(completed).get("entries")).getOrThrow();
        helper.assertTrue(!encounters.finish(boss,BossEncounters.Status.DEFEATED),"replayed completion accepted");
        helper.assertTrue(!encounters.reserve(boss,owner,"morvath_ashen_king",arena),"terminal UUID reused for duplicate reward");
        helper.assertTrue(encounters.reserve(UUID.randomUUID(),owner,"morvath_ashen_king",arena),"completed arena cannot be reused");
        var failed=UUID.randomUUID(); var far=arena.offset(200,0,0);
        helper.assertTrue(encounters.reserve(failed,owner,"morvath_ashen_king",far) && encounters.rollbackUnspawned(failed) && !encounters.conflicts(far),"failed spawn leaked reservation");
        helper.succeed();
    }
    private static net.minecraft.nbt.CompoundTag diskRoundtrip(net.minecraft.nbt.CompoundTag tag){
        java.nio.file.Path directory=null,file=null;
        try {
            directory=java.nio.file.Files.createTempDirectory("pipican-boss-test-"); file=directory.resolve("state.nbt");
            net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
            return net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        }catch(java.io.IOException error){ throw new IllegalStateException("Boss test disk roundtrip failed",error); }
        finally {
            try { if(file!=null) java.nio.file.Files.deleteIfExists(file); if(directory!=null) java.nio.file.Files.deleteIfExists(directory); }
            catch(java.io.IOException error){ throw new IllegalStateException("Boss fixture cleanup failed",error); }
        }
    }
    private static void entityRoundtrip(GameTestHelper helper){
        helper.assertTrue(BossCatalog.all().size()==27,"boss metadata catalog incomplete");
        helper.assertTrue(BossCatalog.MORVATH.matchesBiome(Pipican.id("protected/ashlands"))
            && !BossCatalog.MORVATH.matchesBiome(Pipican.id("crimson_wilds")),"boss region metadata crosses unrelated biomes");
        var level=helper.getLevel();
        var entity=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND);
        helper.assertTrue(entity!=null && entity.getMaxHealth()==600,"entity registration or attributes missing");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(output); var tag=output.buildResult();
        tag.putInt("BossPhase",2); tag.putInt("BossAbility",BossCombatState.Ability.METEOR_RAIN.ordinal());
        tag.putInt("BossRemaining",65); tag.putInt("BossCooldown",13); tag.putBoolean("BossResolved",false);
        var restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(tag)));
        helper.assertTrue(restored.phase()==2 && restored.ability()==BossCombatState.Ability.METEOR_RAIN && restored.abilityTicksRemaining()==65,"entity combat state did not deserialize");
        var second=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); restored.saveWithoutId(second);
        helper.assertTrue(second.buildResult().getIntOr("BossCooldown",0)==13,"saved cooldown lost");
        helper.assertTrue(!restored.removeWhenFarAway(1000000),"boss can naturally despawn");
        helper.succeed();
    }
    private static void durmagonCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(91000,30,91000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,6,16)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.DURMAGON.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var circuit=new DurmagonCombat(boss);helper.assertTrue(circuit.begin(DurmagonCombat.CIRCUIT,refuge),"Durmagon rune circuit failed to start");var rune=circuit.rune(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Durmagon rune interrupt projectile missing");arrow.setPos(rune.x,rune.y,rune.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);circuit.tick(level);helper.assertTrue(circuit.brokenRunes()==1&&!arrow.isAlive(),"Durmagon runic junction was not interruptible");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var rebound=new DurmagonCombat(boss);helper.assertTrue(rebound.begin(DurmagonCombat.REBOUNDS,refuge),"Durmagon forge rebound failed");var mark=rebound.reboundPoint(0,7);danger.snapTo(mark.x,mark.y,mark.z);refreshMockPositions(level,refuge,danger);for(int i=0;i<49;i++)rebound.tick(level);helper.assertTrue(danger.getHealth()==20,"Durmagon rebound hit before telegraph");rebound.tick(level);helper.assertTrue(danger.getHealth()<20,"Durmagon announced rebound path failed to hit");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var stamp=new DurmagonCombat(boss);helper.assertTrue(stamp.updatePhase(500,1000)&&stamp.begin(DurmagonCombat.STAMP,refuge),"Durmagon phase-two hammer stamp failed");var safe=stamp.rune(stamp.safePad());refuge.snapTo(safe.x,safe.y,safe.z);var unsafe=stamp.rune((stamp.safePad()+1)%4);danger.snapTo(unsafe.x,unsafe.y,unsafe.z);for(int i=0;i<59;i++)stamp.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Durmagon stamp resolved before warning");stamp.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Durmagon safe rune-pad counterplay failed");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void durmagonEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(94000,32,94000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));
        for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.DURMAGON_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("protected/greyspine_mountains")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Greyspine biome fixture failed");helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Greyspine altar summon failed");var boss=level.getEntitiesOfClass(DurmagonEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Durmagon spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("durmagon_runic_titan"),"Durmagon encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.DURMAGON_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("runic_iron").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Durmagon trophy/Runic Iron rewards missing");helper.succeed();
        }finally{level.getServer().getPlayerList().remove(owner);}
    }
    private static void syltheraCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(97000,30,97000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,6,16)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.SYLTHERA.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var spores=new SyltheraCombat(boss);helper.assertTrue(spores.updatePhase(550,1100)&&spores.begin(SyltheraCombat.SPORES,refuge),"Sylthera spore sequence failed to start");var safe=spores.safeSporePosition();refuge.snapTo(safe.x,safe.y,safe.z);var unsafe=origin.offset(0,0,3);danger.snapTo(unsafe.getX()+.5,unsafe.getY(),unsafe.getZ()+.5);for(int i=0;i<39;i++)spores.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Sylthera spores hit before sequence warning");spores.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Sylthera shown spore refuge failed");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var nursery=new SyltheraCombat(boss);helper.assertTrue(nursery.begin(SyltheraCombat.NURSERY,refuge),"Sylthera root nursery failed to start");var seed=nursery.seedling(1);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Sylthera seedling interrupt projectile missing");arrow.setPos(seed.x,seed.y,seed.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);nursery.tick(level);helper.assertTrue(nursery.clearedSeedlings()==1&&!arrow.isAlive(),"Sylthera seedling was not destructible");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var bloom=new SyltheraCombat(boss);helper.assertTrue(bloom.begin(SyltheraCombat.BLOOM,refuge),"Sylthera inhalation bloom failed");var line=bloom.bloomPoint(10);danger.snapTo(line.x,line.y,line.z);var flank=origin.offset(11,0,0);refuge.snapTo(flank.getX()+.5,flank.getY(),flank.getZ()+.5);for(int i=0;i<59;i++)bloom.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Sylthera bloom resolved before tell");bloom.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Sylthera directional bloom counterplay failed");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void syltheraEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(99000,32,99000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.SYLTHERA_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("verdant_depths")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Verdant Depths biome fixture failed");helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Verdant Depths altar summon failed");var boss=level.getEntitiesOfClass(SyltheraEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Sylthera spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("sylthera_primordial_bloom"),"Sylthera encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.SYLTHERA_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("lifebloom_crystal").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Sylthera trophy/Lifebloom rewards missing");helper.succeed();
        }finally{level.getServer().getPlayerList().remove(owner);}
    }
    private static void lumynaraCombat(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new net.minecraft.core.BlockPos(102000,30,102000));for(var p:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,6,16)))level.setBlock(p,p.getY()==origin.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);var refuge=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var danger=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);level.addFreshEntity(refuge);level.addFreshEntity(danger);var boss=BossRegistries.LUMYNARA.get().create(level,EntitySpawnReason.COMMAND);boss.snapTo(origin.getX(),origin.getY(),origin.getZ());boss.setNoAi(true);level.addFreshEntity(boss);
        try{refuge.snapTo(origin.getX(),origin.getY(),origin.getZ()+8);var gaze=new LumynaraCombat(boss);helper.assertTrue(gaze.updatePhase(600,1200)&&gaze.begin(LumynaraCombat.GAZE,refuge),"Lumynara luminous gaze failed to start");var beam=gaze.gazePoint(10);danger.snapTo(beam.x,beam.y,beam.z);refuge.snapTo(origin.getX(),origin.getY(),origin.getZ());for(int i=0;i<39;i++)gaze.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Lumynara gaze struck before shutter cue");gaze.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Lumynara locked gaze / phase echo failed");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var waves=new LumynaraCombat(boss);helper.assertTrue(waves.begin(LumynaraCombat.WAVES,refuge),"Lumynara choral waves failed to start");var safe=waves.wavePoint(waves.safeRadius(),0);refuge.snapTo(safe.x,safe.y,safe.z);var impact=waves.wavePoint(3.7,0);danger.snapTo(impact.x,impact.y,impact.z);for(int i=0;i<49;i++)waves.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Lumynara harmonic waves resolved before warning");waves.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Lumynara harmonic safe-band counterplay failed");
            refuge.setHealth(20);refuge.invulnerableTime=0;danger.setHealth(20);danger.invulnerableTime=0;var lanterns=new LumynaraCombat(boss);helper.assertTrue(lanterns.begin(LumynaraCombat.LANTERNS,refuge),"Lumynara lantern convergence failed to start");var source=lanterns.lantern(0);var arrow=net.minecraft.world.entity.EntityType.ARROW.create(level,EntitySpawnReason.COMMAND);helper.assertTrue(arrow!=null,"Lumynara lantern interrupt projectile missing");arrow.setPos(source.x,source.y,source.z);arrow.setNoGravity(true);level.addFreshEntity(arrow);lanterns.tick(level);helper.assertTrue(lanterns.brokenLanterns()==1&&!arrow.isAlive(),"Lumynara lantern node was not interruptible");var refugePoint=lanterns.lanternRayPoint(0,10);refuge.snapTo(refugePoint.x,refugePoint.y,refugePoint.z);var dangerPoint=lanterns.lanternRayPoint(1,10);danger.snapTo(dangerPoint.x,dangerPoint.y,dangerPoint.z);for(int i=0;i<58;i++)lanterns.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()==20,"Lumynara lantern rays resolved before warning");lanterns.tick(level);helper.assertTrue(refuge.getHealth()==20&&danger.getHealth()<20,"Lumynara interrupted lantern ray refuge failed");helper.succeed();
        }finally{refuge.discard();danger.discard();boss.discard();}
    }
    private static void lumynaraEncounter(GameTestHelper helper){
        var level=helper.getLevel();var altar=helper.absolutePos(new net.minecraft.core.BlockPos(104000,32,104000));var arena=altar.north(6);for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8)level.getChunkAt(arena.offset(x,0,z));for(var p:net.minecraft.core.BlockPos.betweenClosed(altar.offset(-8,-1,-14),altar.offset(8,6,3)))level.setBlock(p,p.getY()==altar.getY()-1?net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);level.setBlock(altar,BossContent.ALTAR.get().defaultBlockState(),2);var owner=helper.makeMockServerPlayerInLevel();owner.snapTo(altar.getX()+2,altar.getY(),altar.getZ());var sigil=new net.minecraft.world.item.ItemStack(BossContent.LUMYNARA_SIGIL.get());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sigil);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(altar),net.minecraft.core.Direction.UP,altar,false);
        try{var state=level.getBlockState(altar);var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("shimmering_reefs")));helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,altar.offset(-8,-8,-14),altar.offset(8,8,3),biome).left().isPresent(),"Shimmering Reefs biome fixture failed");helper.assertTrue(state.useItemOn(sigil,level,owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.SUCCESS&&sigil.isEmpty(),"Shimmering Reefs altar summon failed");var boss=level.getEntitiesOfClass(LumynaraEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(5)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow(()->new IllegalStateException("Lumynara spawn missing"));helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("lumynara_abyssal_oracle"),"Lumynara encounter type not persisted");boss.hurtServer(level,boss.damageSources().genericKill(),10000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(8));helper.assertTrue(drops.stream().anyMatch(item->item.getItem().is(BossContent.LUMYNARA_TROPHY.get()))&&drops.stream().filter(item->item.getItem().is(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("abyssal_shard").get())).mapToInt(item->item.getItem().getCount()).sum()==8,"Lumynara trophy/Abyssal Shard rewards missing");helper.succeed();
        }finally{level.getServer().getPlayerList().remove(owner);}
    }
}

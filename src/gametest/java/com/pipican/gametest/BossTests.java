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
        Map.entry("nocthar_deep_encounter",BossTests::noctharDeepEncounter));
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
        helper.assertTrue(BossDesigns.all().size()==27 && BossDesigns.all().stream().filter(BossDesigns.Design::functional).count()==7,"functional boss design count differs from implemented bindings");
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
        String[] ids={"ritual_altar","ritual_core","morvath_sigil","frostbound_colossus_sigil","eldaroth_treant_sigil","fallen_king_sigil","kragmor_canyon_warlord_sigil","nocthar_void_sovereign_sigil"};
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
            var boss=level.getEntitiesOfClass(NoctharEntity.class,new net.minecraft.world.phys.AABB(arena).inflate(4)).stream().filter(RegionalBossEntity::isAlive).findFirst().orElseThrow();
            helper.assertTrue(BossEncounters.get(level).find(boss.getUUID()).orElseThrow().type().equals("nocthar_void_sovereign"),"Nocthar encounter type was not persisted");
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
        front.setHealth(20);front.invulnerableTime=0;front.snapTo(origin.getX()+7,origin.getY(),origin.getZ());dodge.setHealth(20);dodge.invulnerableTime=0;dodge.snapTo(origin.getX()+7,origin.getY(),origin.getZ());
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
        var wells=new NoctharCombat(nocthar);helper.assertTrue(wells.begin(NoctharCombat.WELLS,front),"Nocthar gravity wells failed to start");front.snapTo(origin.getX()+6,origin.getY(),origin.getZ()+3.5);
        for(int i=0;i<49;i++)wells.tick(level);helper.assertTrue(front.getHealth()==20,"Nocthar gravity well damaged before charge");wells.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20&&front.getDeltaMovement().horizontalDistanceSqr()>0,"Nocthar bounded gravity pull/damage or exit-boundary counterplay failed");
        front.setHealth(20);front.invulnerableTime=0;dodge.setHealth(20);dodge.invulnerableTime=0;
        var eclipse=new NoctharCombat(nocthar);helper.assertTrue(eclipse.begin(NoctharCombat.ECLIPSE,front),"Nocthar eclipse lanes failed to start");
        double beam=Math.PI/8,safe=Math.PI*3/8;front.snapTo(origin.getX()+Math.cos(beam)*6,origin.getY(),origin.getZ()+Math.sin(beam)*6);dodge.snapTo(origin.getX()+Math.cos(safe)*6,origin.getY(),origin.getZ()+Math.sin(safe)*6);
        for(int i=0;i<59;i++)eclipse.tick(level);helper.assertTrue(front.getHealth()==20,"Nocthar eclipse beam fired before lane warning");eclipse.tick(level);
        helper.assertTrue(front.getHealth()<20&&dodge.getHealth()==20,"Nocthar rotating beam lane or unshaded corridor failed");
        nocthar.setHealth(nocthar.getMaxHealth()/2);nocthar.tickCombat(level);helper.assertTrue(nocthar.phase()==2,"Nocthar phase two did not latch");
        output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());nocthar.saveWithoutId(output);var noctharReload=BossRegistries.NOCTHAR.get().create(level,EntitySpawnReason.LOAD);
        noctharReload.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),diskRoundtrip(output.buildResult())));helper.assertTrue(noctharReload.phase()==2,"Nocthar phase or spatial state was lost on reload");
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
}

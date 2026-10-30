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
    private static final Map<String,Consumer<GameTestHelper>> TESTS=Map.of(
        "boss_combat_reload",BossTests::combatReload,
        "boss_phase_latch",BossTests::phaseLatch,
        "boss_entity_roundtrip",BossTests::entityRoundtrip,
        "boss_encounter_reservations",BossTests::encounterReservations,
        "boss_summoning_lifecycle",BossTests::summoningLifecycle,
        "boss_unload_and_cleanup",BossTests::unloadAndCleanup);
    @SubscribeEvent public static void functions(RegisterEvent event){
        event.register(Registries.TEST_FUNCTION,registry->TESTS.forEach((name,fn)->registry.register(Pipican.id(name),fn)));
    }
    @SubscribeEvent public static void tests(RegisterGameTestsEvent event){
        Holder<TestEnvironmentDefinition<?>> env=event.registerEnvironment(Pipican.id("boss_test"),new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name->event.registerTest(Pipican.id(name),new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION,Pipican.id(name)),
            new TestData<>(env,net.minecraft.resources.Identifier.withDefaultNamespace("empty"),100,0,true))));
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
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(arena.offset(-2,-1,-2),arena.offset(2,5,2)))
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

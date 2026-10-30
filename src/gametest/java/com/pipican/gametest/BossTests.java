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
        "boss_entity_roundtrip",BossTests::entityRoundtrip);
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
    private static void entityRoundtrip(GameTestHelper helper){
        var level=helper.getLevel();
        var entity=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.COMMAND);
        helper.assertTrue(entity!=null && entity.getMaxHealth()==600,"entity registration or attributes missing");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(output); var tag=output.buildResult();
        tag.putInt("BossPhase",2); tag.putInt("BossAbility",BossCombatState.Ability.METEOR_RAIN.ordinal());
        tag.putInt("BossRemaining",65); tag.putInt("BossCooldown",13); tag.putBoolean("BossResolved",false);
        var restored=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
        helper.assertTrue(restored.phase()==2 && restored.ability()==BossCombatState.Ability.METEOR_RAIN && restored.abilityTicksRemaining()==65,"entity combat state did not deserialize");
        var second=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); restored.saveWithoutId(second);
        helper.assertTrue(second.buildResult().getIntOr("BossCooldown",0)==13,"saved cooldown lost");
        helper.assertTrue(!restored.removeWhenFarAway(1000000),"boss can naturally despawn");
        helper.succeed();
    }
}

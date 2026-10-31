package com.pipican.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Reef colossus with a stable gate opening, delayed sonar and shootable rolling urchins. */
public final class CorallithEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(CorallithEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",CorallithCombat.GATES,CorallithCombat.ECHO,CorallithCombat.URCHINS);
    private final CorallithCombat combat=new CorallithCombat(this);private int sequence,transitionTicks,summoningTicks;
    public CorallithEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"corallith_reef_colossus",BossEvent.BossBarColor.BLUE,250);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("corallith_reef_colossus");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,16).add(Attributes.MOVEMENT_SPEED,0.19).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.96);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int corallithAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=68;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,getX(),getY()+0.25,getZ(),14,0.8,0.2,0.8,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=80;playSound(SoundEvents.CONDUIT_ACTIVATE,1.3f,0.55f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.NAUTILUS,getX(),getY()+1,getZ(),16,0.9,0.6,0.9,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=CorallithCombat.GATES;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?0.9:0.76);}else getNavigation().stop();combat.tick(level);
        String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(CorallithCombat.GATES)?2:active.equals(CorallithCombat.ECHO)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("CorallithSequence",sequence);o.putInt("CorallithTransition",transitionTicks);o.putInt("CorallithSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("CorallithSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("CorallithTransition",0),0,80);summoningTicks=Math.clamp(i.getIntOr("CorallithSummoning",0),0,68);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(CorallithCombat.GATES)?2:a.equals(CorallithCombat.ECHO)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tidalite").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.CORALLITH_TROPHY.get()));}
}

package com.pipican.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-side Frostbound encounter. Registration remains gated on rendering and rewards. */
public final class FrostboundEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(FrostboundEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of(FrostboundCombat.BASIC,FrostboundCombat.FISSURE,FrostboundCombat.WHEEL,FrostboundCombat.LANCES);
    private final FrostboundCombat frost=new FrostboundCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public FrostboundEntity(EntityType<? extends RegionalBossEntity> type,Level level){
        super(type,level,"frostbound_colossus",BossEvent.BossBarColor.BLUE,200);
    }
    public static AttributeSupplier.Builder frostAttributes(){
        var design=BossDesigns.require("frostbound_colossus");
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,design.health())
            .add(Attributes.ATTACK_DAMAGE,design.damage()).add(Attributes.ARMOR,14).add(Attributes.MOVEMENT_SPEED,0.20)
            .add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){ super.defineSynchedData(builder); builder.define(ACTION,0); }
    public int frostAction(){ return entityData.get(ACTION); }
    public FrostboundCombat frostCombat(){ return frost; }
    @Override public boolean beginAbility(BossCombatState.Ability ability,net.minecraft.world.entity.LivingEntity target){ return false; }
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){
            summoningTicks--;
            getNavigation().stop();
            if(summoningTicks%4==0) level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.5,getZ(),10,1,1,1,0.01);
            entityData.set(ACTION,summoningTicks>0 ? 6 : 0);
            syncCombat(frost.clock().phase(),summoningTicks>0 ? BossCombatState.Ability.SUMMONING : BossCombatState.Ability.NONE,summoningTicks);
            return;
        }
        if(frost.updatePhase(getHealth(),getMaxHealth())) transitionTicks=60;
        if(transitionTicks>0){
            getNavigation().stop();
            if(transitionTicks%5==0) level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+2,getZ(),12,1,1.5,1,0.02);
            transitionTicks--;
            syncCombat(2,transitionTicks>0 ? BossCombatState.Ability.PHASE_CHANGE : BossCombatState.Ability.NONE,transitionTicks);
            entityData.set(ACTION,transitionTicks>0 ? 5 : 0);
            return;
        }
        var target=getTarget();
        if(target!=null && (!target.isAlive() || !insideArena(target.position()))){ setTarget(null); target=null; }
        if(target!=null && frost.clock().active().isEmpty() && frost.clock().cooldown()==0 && hasLineOfSight(target)){
            String attack=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));
            if(attack.equals(FrostboundCombat.BASIC) && distanceToSqr(target)>25) attack=FrostboundCombat.FISSURE;
            if(frost.begin(attack,target)) sequence=(sequence+1)%PATTERN.size();
        }
        if(frost.clock().active().isEmpty()){
            if(target!=null) getNavigation().moveTo(target,frost.clock().phase()==2 ? 1.1 : 1.0);
        }else getNavigation().stop();
        frost.tick(level);
        String active=frost.clock().active();
        int action=active.isEmpty() ? 0 : PATTERN.indexOf(active)+1;
        entityData.set(ACTION,action);
        syncCombat(frost.clock().phase(),BossCombatState.Ability.NONE,frost.clock().remaining());
    }
    @Override void activateEncounter(){ super.activateEncounter(); summoningTicks=60; }
    @Override protected void addAdditionalSaveData(ValueOutput output){
        super.addAdditionalSaveData(output); frost.save(output);
        output.putInt("FrostSequence",sequence); output.putInt("FrostTransition",transitionTicks); output.putInt("FrostSummoning",summoningTicks);
    }
    @Override protected void readAdditionalSaveData(ValueInput input){
        super.readAdditionalSaveData(input); frost.load(input);
        sequence=Math.floorMod(input.getIntOr("FrostSequence",0),PATTERN.size());
        transitionTicks=Math.clamp(input.getIntOr("FrostTransition",0),0,60);
        summoningTicks=Math.clamp(input.getIntOr("FrostSummoning",0),0,60);
        String active=frost.clock().active();
        entityData.set(ACTION,summoningTicks>0 ? 6 : transitionTicks>0 ? 5 : active.isEmpty() ? 0 : PATTERN.indexOf(active)+1);
        syncCombat(frost.clock().phase(),summoningTicks>0 ? BossCombatState.Ability.SUMMONING : transitionTicks>0 ? BossCombatState.Ability.PHASE_CHANGE : BossCombatState.Ability.NONE,
            summoningTicks>0 ? summoningTicks : transitionTicks>0 ? transitionTicks : frost.clock().remaining());
    }
    /** No placeholder Morvath trophy can leak from this incomplete encounter. */
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("frostcrystal").get(),8));
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.FROSTBOUND_TROPHY.get()));
    }
}

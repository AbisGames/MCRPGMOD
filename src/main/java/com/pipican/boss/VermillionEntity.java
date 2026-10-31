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

/** Blood Monarch with persistent channel attacks and a phase-two exposed window. */
public final class VermillionEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(VermillionEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",VermillionCombat.TETHERS,VermillionCombat.ORBITS,VermillionCombat.COURT);
    private final VermillionCombat combat=new VermillionCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public VermillionEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"vermillion_blood_monarch",BossEvent.BossBarColor.RED,210);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("vermillion_blood_monarch");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,16).add(Attributes.MOVEMENT_SPEED,0.29)
        .add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.85);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int vermillionAction(){return entityData.get(ACTION);}
    public boolean isExposed(){return combat.exposed();}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=65;}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return super.hurtServer(level,source,combat.exposed()?amount*1.25f:amount);}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.CRIMSON_SPORE,getX(),getY()+1,getZ(),12,0.8,0.6,0.8,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=75;playSound(SoundEvents.WITHER_SPAWN,1.15f,0.8f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%7==0)level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,getX(),getY()+1.8,getZ(),12,0.9,0.7,0.9,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>25)id=VermillionCombat.TETHERS;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.12:0.94);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("VermillionSequence",sequence);out.putInt("VermillionTransition",transitionTicks);out.putInt("VermillionSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("VermillionSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("VermillionTransition",0),0,75);summoningTicks=Math.clamp(in.getIntOr("VermillionSummoning",0),0,65);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:combat.clock().active().isEmpty()?0:PATTERN.indexOf(combat.clock().active())+1);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("bloodstone").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.VERMILLION_TROPHY.get()));
        if(Math.floorMod(getUUID().hashCode(),4)==0)spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.resource.RareResources.MATERIALS.get("bloodcrystal").get()));
    }
}

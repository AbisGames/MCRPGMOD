package com.pipican.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Aetherion stays airborne and uses 3D navigation for its aerial duel. */
public final class AetherionEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(AetherionEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",AetherionCombat.UPDRAFT,AetherionCombat.FEATHERS,AetherionCombat.DIVE);
    private final AetherionCombat combat=new AetherionCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public AetherionEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"aetherion_sky_guardian",BossEvent.BossBarColor.BLUE,300);setNoGravity(true);moveControl=new FlyingMoveControl(this,20,true);}
    @Override protected PathNavigation createNavigation(Level level){var navigation=new FlyingPathNavigation(this,level);navigation.setCanOpenDoors(false);navigation.setCanFloat(true);return navigation;}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("aetherion_sky_guardian");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,12).add(Attributes.FLYING_SPEED,0.36)
        .add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.FOLLOW_RANGE,56).add(Attributes.KNOCKBACK_RESISTANCE,0.7);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int aetherionAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=70;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();double rise=Math.clamp((summoningTicks<55?getY()+8-getY():0.16),0,0.18);setDeltaMovement(getDeltaMovement().x(),rise,getDeltaMovement().z());if(summoningTicks%5==0)level.sendParticles(ParticleTypes.CLOUD,getX(),getY(),getZ(),9,0.7,0.2,0.7,0.01);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=80;playSound(SoundEvents.PHANTOM_FLAP,1.7f,0.55f);}
        if(transitionTicks>0){getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(0.8).add(0,0.035,0));if(transitionTicks%6==0)level.sendParticles(ParticleTypes.END_ROD,getX(),getY(),getZ(),10,0.8,0.5,0.8,0.03);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>36)id=AetherionCombat.UPDRAFT;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();
        }
        if(combat.clock().active().isEmpty()){
            if(target!=null)getNavigation().moveTo(target.getX(),target.getY()+6,target.getZ(),combat.clock().phase()==2?1.2:1.0);
            else{getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(0.82).add(0,Math.sin(tickCount*0.09)*0.006,0));}
        }else{getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(0.7));}
        combat.tick(level);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("AetherionSequence",sequence);out.putInt("AetherionTransition",transitionTicks);out.putInt("AetherionSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("AetherionSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("AetherionTransition",0),0,80);summoningTicks=Math.clamp(in.getIntOr("AetherionSummoning",0),0,70);setNoGravity(true);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:combat.clock().active().isEmpty()?0:PATTERN.indexOf(combat.clock().active())+1);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skycrystal").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.AETHERION_TROPHY.get()));
        if(Math.floorMod(getUUID().hashCode(),4)==0)spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.resource.RareResources.MATERIALS.get("celestium").get()));
    }
}

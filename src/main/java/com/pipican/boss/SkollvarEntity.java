package com.pipican.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server AI and persistent phase clock for Sköllvar. */
public final class SkollvarEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(SkollvarEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",SkollvarCombat.CHARGE,SkollvarCombat.MIRAGES,SkollvarCombat.PROWL);
    private final SkollvarCombat hunt=new SkollvarCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public SkollvarEntity(EntityType<? extends RegionalBossEntity> type,Level level){ super(type,level,"skollvar_winterfang",BossEvent.BossBarColor.PURPLE,162); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){
        var design=BossDesigns.require("skollvar_winterfang");
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,design.health())
            .add(Attributes.ATTACK_DAMAGE,design.damage()).add(Attributes.ARMOR,8).add(Attributes.MOVEMENT_SPEED,0.34)
            .add(Attributes.FOLLOW_RANGE,44).add(Attributes.KNOCKBACK_RESISTANCE,0.65);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){ super.defineSynchedData(builder); builder.define(ACTION,0); }
    public int skollAction(){ return entityData.get(ACTION); }
    @Override public boolean beginAbility(BossCombatState.Ability ability,net.minecraft.world.entity.LivingEntity target){ return false; }
    @Override void activateEncounter(){ super.activateEncounter(); summoningTicks=50; }
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){
            summoningTicks--; getNavigation().stop();
            if(summoningTicks%5==0) level.sendParticles(ParticleTypes.CLOUD,getX(),getY()+0.4,getZ(),10,0.8,0.3,0.8,0.01);
            entityData.set(ACTION,summoningTicks>0?6:0); return;
        }
        if(hunt.updatePhase(getHealth(),getMaxHealth())) transitionTicks=60;
        if(transitionTicks>0){
            getNavigation().stop(); if(transitionTicks%8==0) playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,1.4f,0.65f);
            transitionTicks--; entityData.set(ACTION,transitionTicks>0?5:0);
            syncCombat(2,transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,transitionTicks); return;
        }
        var target=getTarget();
        if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&hunt.clock().active().isEmpty()&&hunt.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));
            if(id.equals("basic")&&distanceToSqr(target)>16) id=SkollvarCombat.CHARGE;
            if(hunt.begin(id,target)) sequence=(sequence+1)%PATTERN.size();
        }
        if(hunt.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,hunt.clock().phase()==2?1.35:1.15);}
        else getNavigation().stop();
        hunt.tick(level); String active=hunt.clock().active();
        entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);
        syncCombat(hunt.clock().phase(),BossCombatState.Ability.NONE,hunt.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out); hunt.save(out); out.putInt("SkollSequence",sequence);
        out.putInt("SkollTransition",transitionTicks); out.putInt("SkollSummoning",summoningTicks);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in); hunt.load(in); sequence=Math.floorMod(in.getIntOr("SkollSequence",0),PATTERN.size());
        transitionTicks=Math.clamp(in.getIntOr("SkollTransition",0),0,60); summoningTicks=Math.clamp(in.getIntOr("SkollSummoning",0),0,50);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:hunt.clock().active().isEmpty()?0:PATTERN.indexOf(hunt.clock().active())+1);
        syncCombat(hunt.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,
            summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:hunt.clock().remaining());
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("wintersteel").get(),8));
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.SKOLLVAR_TROPHY.get()));
    }
}

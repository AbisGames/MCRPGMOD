package com.pipican.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Canyon warlord with persisted hazard queues and phase transition. */
public final class KragmorEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(KragmorEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",KragmorCombat.ROCKFALL,KragmorCombat.TETHERS,KragmorCombat.LEAP);
    private final KragmorCombat combat=new KragmorCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public KragmorEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"kragmor_canyon_warlord",BossEvent.BossBarColor.YELLOW,240);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("kragmor_canyon_warlord");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,18).add(Attributes.MOVEMENT_SPEED,0.26)
        .add(Attributes.FOLLOW_RANGE,44).add(Attributes.KNOCKBACK_RESISTANCE,0.95);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int kragmorAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=60;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.POOF,getX(),getY()+1,getZ(),10,0.7,0.5,0.7,0.01);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=80;playSound(SoundEvents.RAVAGER_ROAR,1.6f,0.6f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%8==0)level.sendParticles(ParticleTypes.POOF,getX(),getY()+1.5,getZ(),8,0.8,0.6,0.8,0.01);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>25)id=KragmorCombat.ROCKFALL;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();
        }
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.02:0.82);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("KragSequence",sequence);out.putInt("KragTransition",transitionTicks);out.putInt("KragSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("KragSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("KragTransition",0),0,80);summoningTicks=Math.clamp(in.getIntOr("KragSummoning",0),0,60);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:combat.clock().active().isEmpty()?0:PATTERN.indexOf(combat.clock().active())+1);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("redrock_iron").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.KRAGMOR_TROPHY.get()));}
}

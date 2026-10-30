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

/** Persistent royal boss encounter and server-side combat loop. */
public final class FallenKingEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(FallenKingEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",FallenKingCombat.DUEL,FallenKingCombat.CAVALRY,FallenKingCombat.REGALIA);
    private final FallenKingCombat combat=new FallenKingCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public FallenKingEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"fallen_king",BossEvent.BossBarColor.RED,190);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){
        var d=BossDesigns.require("fallen_king");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health())
            .add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,14).add(Attributes.MOVEMENT_SPEED,0.28)
            .add(Attributes.FOLLOW_RANGE,44).add(Attributes.KNOCKBACK_RESISTANCE,0.85);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int fallenAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=55;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1,getZ(),10,0.7,0.5,0.7,0.01);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=70;playSound(SoundEvents.WITHER_SKELETON_AMBIENT,1.6f,0.55f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%8==0)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+1.8,getZ(),8,0.8,0.7,0.8,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>25)id=FallenKingCombat.CAVALRY;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();
        }
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.12:0.92);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);
        syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("FallenSequence",sequence);out.putInt("FallenTransition",transitionTicks);out.putInt("FallenSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("FallenSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("FallenTransition",0),0,70);summoningTicks=Math.clamp(in.getIntOr("FallenSummoning",0),0,55);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:combat.clock().active().isEmpty()?0:PATTERN.indexOf(combat.clock().active())+1);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("royal_silver").get(),8));
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.FALLEN_KING_TROPHY.get()));
    }
}

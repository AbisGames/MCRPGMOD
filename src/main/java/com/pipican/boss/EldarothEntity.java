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

/** Server controlled ancient treant with a persistent regional attack clock. */
public final class EldarothEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(EldarothEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",EldarothCombat.ROOTS,EldarothCombat.PODS,EldarothCombat.BRANCH);
    private final EldarothCombat combat=new EldarothCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public EldarothEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"eldaroth_treant",BossEvent.BossBarColor.GREEN,220);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){
        var d=BossDesigns.require("eldaroth_treant");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,12)
            .add(Attributes.MOVEMENT_SPEED,0.22).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,0.9);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int eldarothAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=70;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,getX(),getY()+1,getZ(),12,1,0.8,1,0.01);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=80;playSound(SoundEvents.ENDER_DRAGON_GROWL,1.2f,0.65f);}
        if(transitionTicks>0){getNavigation().stop();transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>25)id=EldarothCombat.PODS;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();
        }
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?0.8:0.68);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:PATTERN.indexOf(active)+1);
        syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("EldarothSequence",sequence);out.putInt("EldarothTransition",transitionTicks);out.putInt("EldarothSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("EldarothSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("EldarothTransition",0),0,80);summoningTicks=Math.clamp(in.getIntOr("EldarothSummoning",0),0,70);
        entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:combat.clock().active().isEmpty()?0:PATTERN.indexOf(combat.clock().active())+1);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mooncrystal").get(),8));
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.ELDAROTH_TROPHY.get()));
    }
}

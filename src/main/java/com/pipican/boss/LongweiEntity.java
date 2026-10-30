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

/** Jade dragon with independent, persistent breath, spiral and orb attacks. */
public final class LongweiEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(LongweiEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",LongweiCombat.RIBBON,LongweiCombat.SPIRAL,LongweiCombat.ORBITS);
    private final LongweiCombat combat=new LongweiCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public LongweiEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"longwei_jade_dragon",BossEvent.BossBarColor.GREEN,165);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("longwei_jade_dragon");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,13).add(Attributes.MOVEMENT_SPEED,0.27)
        .add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.8);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int longweiAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=60;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,getX(),getY()+1,getZ(),12,0.8,0.55,0.8,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=72;playSound(SoundEvents.ENDER_DRAGON_GROWL,1.2f,0.9f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%6==0)level.sendParticles(ParticleTypes.END_ROD,getX(),getY()+1.2,getZ(),14,0.9,0.7,0.9,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){
            String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>36)id=LongweiCombat.RIBBON;
            if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.08:0.9);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();int action=active.isEmpty()?0:active.equals("basic")?1:active.equals(LongweiCombat.RIBBON)?2:active.equals(LongweiCombat.SPIRAL)?3:4;
        entityData.set(ACTION,action);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("LongweiSequence",sequence);out.putInt("LongweiTransition",transitionTicks);out.putInt("LongweiSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("LongweiSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("LongweiTransition",0),0,72);summoningTicks=Math.clamp(in.getIntOr("LongweiSummoning",0),0,60);
        String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(LongweiCombat.RIBBON)?2:active.equals(LongweiCombat.SPIRAL)?3:4);
        syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("jade_essence").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.LONGWEI_TROPHY.get()));}
}

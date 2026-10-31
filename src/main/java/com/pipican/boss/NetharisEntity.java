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

/** River guardian with segmented water gates and a persisted, interruptible current ritual. */
public final class NetharisEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(NetharisEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",NetharisCombat.GATES,NetharisCombat.JAVELINS,NetharisCombat.TRIBUTARY);
    private final NetharisCombat combat=new NetharisCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public NetharisEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"netharis_river_guardian",BossEvent.BossBarColor.BLUE,215);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("netharis_river_guardian");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,13).add(Attributes.MOVEMENT_SPEED,0.24)
        .add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.9);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int netharisAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return super.hurtServer(level,source,amount*combat.damageMultiplier(source));}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=64;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+0.25,getZ(),14,0.75,0.25,0.75,0.035);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=74;playSound(SoundEvents.CONDUIT_ACTIVATE,1.35f,0.6f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,getX(),getY()+0.8,getZ(),14,0.8,0.7,0.8,0.025);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>32)id=NetharisCombat.GATES;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.0:0.84);}else getNavigation().stop();combat.tick(level);
        String active=combat.clock().active();int action=active.isEmpty()?0:active.equals("basic")?1:active.equals(NetharisCombat.GATES)?2:active.equals(NetharisCombat.JAVELINS)?3:4;entityData.set(ACTION,action);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("NetharisSequence",sequence);out.putInt("NetharisTransition",transitionTicks);out.putInt("NetharisSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("NetharisSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("NetharisTransition",0),0,74);summoningTicks=Math.clamp(in.getIntOr("NetharisSummoning",0),0,64);String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(NetharisCombat.GATES)?2:active.equals(NetharisCombat.JAVELINS)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunstone").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.NETHARIS_TROPHY.get()));}
}

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

/** Sunken emperor with directional mirror defense and pressure weapons. */
public final class AureliosEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(AureliosEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",AureliosCombat.MIRRORS,AureliosCombat.TITHE,AureliosCombat.GRID);
    private final AureliosCombat combat=new AureliosCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public AureliosEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"aurelios_sunken_emperor",BossEvent.BossBarColor.BLUE,160);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("aurelios_sunken_emperor");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,12).add(Attributes.MOVEMENT_SPEED,0.24)
        .add(Attributes.FOLLOW_RANGE,46).add(Attributes.KNOCKBACK_RESISTANCE,0.78);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int aureliosAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=60;}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return super.hurtServer(level,source,amount*combat.damageMultiplier(source));}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.BUBBLE,getX(),getY()+1,getZ(),14,0.75,0.55,0.75,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=70;playSound(SoundEvents.CONDUIT_AMBIENT,1.4f,0.58f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.GLOW,getX(),getY()+1.25,getZ(),18,0.8,0.65,0.8,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=AureliosCombat.GRID;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1:0.84);}else getNavigation().stop();combat.tick(level);
        String active=combat.clock().active();int action=active.isEmpty()?0:active.equals("basic")?1:active.equals(AureliosCombat.MIRRORS)?2:active.equals(AureliosCombat.GRID)?3:4;entityData.set(ACTION,action);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("AureliosSequence",sequence);out.putInt("AureliosTransition",transitionTicks);out.putInt("AureliosSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("AureliosSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("AureliosTransition",0),0,70);summoningTicks=Math.clamp(in.getIntOr("AureliosSummoning",0),0,60);String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(AureliosCombat.MIRRORS)?2:active.equals(AureliosCombat.GRID)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("pearlsilver").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.AURELIOS_TROPHY.get()));}
}

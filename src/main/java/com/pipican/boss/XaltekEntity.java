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

/** Emerald-jungle predator whose seed pods, tongue and landing marks survive save/reload. */
public final class XaltekEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(XaltekEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",XaltekCombat.SNARES,XaltekCombat.TONGUE,XaltekCombat.POUNCE);
    private final XaltekCombat combat=new XaltekCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public XaltekEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"xaltek_emerald_devourer",BossEvent.BossBarColor.GREEN,238);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("xaltek_emerald_devourer");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,13).add(Attributes.MOVEMENT_SPEED,0.29).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.88);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int xaltekAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return super.hurtServer(level,source,amount*combat.damageMultiplier());}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=64;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.COMPOSTER,getX(),getY()+0.4,getZ(),14,0.8,0.35,0.8,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=76;playSound(SoundEvents.ENDER_DRAGON_GROWL,1.05f,0.75f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,getX(),getY()+1,getZ(),14,0.9,0.6,0.9,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=XaltekCombat.POUNCE;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.06:0.92);}else getNavigation().stop();combat.tick(level);
        String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(XaltekCombat.SNARES)?2:active.equals(XaltekCombat.TONGUE)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("XaltekSequence",sequence);out.putInt("XaltekTransition",transitionTicks);out.putInt("XaltekSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("XaltekSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("XaltekTransition",0),0,76);summoningTicks=Math.clamp(in.getIntOr("XaltekSummoning",0),0,64);String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(XaltekCombat.SNARES)?2:active.equals(XaltekCombat.TONGUE)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("verdantite").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.XALTEK_TROPHY.get()));}
}

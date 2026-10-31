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

/** Abyssal tyrant with its own pressure, trident and mine encounter state. */
public final class ThalassorEntity extends RegionalBossEntity {
 private static final EntityDataAccessor<Integer>ACTION=SynchedEntityData.defineId(ThalassorEntity.class,EntityDataSerializers.INT);
 private static final List<String>PATTERN=List.of("basic",ThalassorCombat.FAN,ThalassorCombat.PRESSURE,ThalassorCombat.MINES);
 private final ThalassorCombat combat=new ThalassorCombat(this);private int sequence,transitionTicks,summoningTicks;
 public ThalassorEntity(EntityType<? extends RegionalBossEntity>type,Level level){super(type,level,"thalassor_deep_tyrant",BossEvent.BossBarColor.BLUE,240);}
 public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("thalassor_deep_tyrant");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,18).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,52).add(Attributes.KNOCKBACK_RESISTANCE,.9);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int thalassorAction(){return entityData.get(ACTION);}
 @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
 @Override void activateEncounter(){super.activateEncounter();summoningTicks=72;}
 @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,getX(),getY()+.7,getZ(),16,.8,.5,.8,.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
  if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=90;playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,1.2f,.65f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.BUBBLE,getX(),getY()+1,getZ(),20,.8,.7,.8,.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
  var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=ThalassorCombat.FAN;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
  if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,.78);}else getNavigation().stop();combat.tick(l);String a=combat.clock().active();entityData.set(ACTION,a.isEmpty()?0:a.equals("basic")?1:a.equals(ThalassorCombat.FAN)?2:a.equals(ThalassorCombat.PRESSURE)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("ThalassorSequence",sequence);o.putInt("ThalassorTransition",transitionTicks);o.putInt("ThalassorSummoning",summoningTicks);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("ThalassorSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("ThalassorTransition",0),0,90);summoningTicks=Math.clamp(i.getIntOr("ThalassorSummoning",0),0,72);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(ThalassorCombat.FAN)?2:a.equals(ThalassorCombat.PRESSURE)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
 @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean killed){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("tideglass").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.THALASSOR_TROPHY.get()));}
}

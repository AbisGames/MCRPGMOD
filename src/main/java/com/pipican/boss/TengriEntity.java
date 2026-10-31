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

/** Khan of the khagan steppe; combat and saved timing do not depend on prototype animations. */
public final class TengriEntity extends RegionalBossEntity {
 private static final EntityDataAccessor<Integer>ACTION=SynchedEntityData.defineId(TengriEntity.class,EntityDataSerializers.INT);
 private static final List<String>PATTERN=List.of("basic",TengriCombat.TRIANGLE,TengriCombat.VOLLEY,TengriCombat.BANNER);
 private final TengriCombat combat=new TengriCombat(this);private int sequence,transitionTicks,summoningTicks;
 public TengriEntity(EntityType<? extends RegionalBossEntity>type,Level level){super(type,level,"tengri_champion",BossEvent.BossBarColor.RED,240);}
 public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("tengri_champion");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,13).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,.75);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int tengriAction(){return entityData.get(ACTION);}
 @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
 @Override void activateEncounter(){super.activateEncounter();summoningTicks=60;}
 @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.CLOUD,getX(),getY()+.5,getZ(),12,.7,.2,.7,.01);entityData.set(ACTION,summoningTicks>0?6:0);return;}
  if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=78;playSound(SoundEvents.RAID_HORN.value(),1.2f,.8f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.CRIT,getX(),getY()+1,getZ(),16,.8,.5,.8,.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
  var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>28)id=TengriCombat.TRIANGLE;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
  if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,.92);}else getNavigation().stop();combat.tick(l);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(TengriCombat.TRIANGLE)?2:active.equals(TengriCombat.VOLLEY)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("TengriSequence",sequence);o.putInt("TengriTransition",transitionTicks);o.putInt("TengriSummoning",summoningTicks);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("TengriSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("TengriTransition",0),0,78);summoningTicks=Math.clamp(i.getIntOr("TengriSummoning",0),0,60);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(TengriCombat.TRIANGLE)?2:a.equals(TengriCombat.VOLLEY)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
 @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("skyiron").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.TENGRI_TROPHY.get()));}
}

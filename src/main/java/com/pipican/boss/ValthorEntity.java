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

/** Stormcoast stormbringer with a locked lightning-and-gale attack cycle. */
public final class ValthorEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(ValthorEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",ValthorCombat.CONDUCTORS,ValthorCombat.DONUT,ValthorCombat.GALES);
    private final ValthorCombat combat=new ValthorCombat(this);private int sequence,transitionTicks,summoningTicks;
    public ValthorEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"valthor_stormbringer",BossEvent.BossBarColor.YELLOW,240);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("valthor_stormbringer");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,15).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,.95);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int valthorAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=70;}
    @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+.6,getZ(),14,.7,.4,.7,.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=82;playSound(SoundEvents.LIGHTNING_BOLT_THUNDER,1.2f,.7f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+1,getZ(),18,.9,.7,.9,.03);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=ValthorCombat.CONDUCTORS;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,.84);}else getNavigation().stop();combat.tick(l);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(ValthorCombat.CONDUCTORS)?2:active.equals(ValthorCombat.DONUT)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("ValthorSequence",sequence);o.putInt("ValthorTransition",transitionTicks);o.putInt("ValthorSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("ValthorSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("ValthorTransition",0),0,82);summoningTicks=Math.clamp(i.getIntOr("ValthorSummoning",0),0,70);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(ValthorCombat.CONDUCTORS)?2:a.equals(ValthorCombat.DONUT)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("thunderbasalt").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.VALTHOR_TROPHY.get()));}
}

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

/** Golden savanna behemoth using breakable refraction plates and locked chase lanes. */
public final class AzharEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(AzharEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",AzharCombat.REFRACTION,AzharCombat.STAMPEDE,AzharCombat.KNEEL);
    private final AzharCombat combat=new AzharCombat(this);private int sequence,transitionTicks,summoningTicks;
    public AzharEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"azhar_golden_behemoth",BossEvent.BossBarColor.YELLOW,275);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("azhar_golden_behemoth");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,17).add(Attributes.MOVEMENT_SPEED,0.23).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.97);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int azharAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=72;}
    @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.END_ROD,getX(),getY()+0.45,getZ(),14,0.8,0.3,0.8,0.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=84;playSound(SoundEvents.RAVAGER_ROAR,1.25f,0.68f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.FLAME,getX(),getY()+1,getZ(),16,0.8,0.7,0.8,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>32)id=AzharCombat.STAMPEDE;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?0.98:0.82);}else getNavigation().stop();combat.tick(l);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(AzharCombat.REFRACTION)?2:active.equals(AzharCombat.STAMPEDE)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("AzharSequence",sequence);o.putInt("AzharTransition",transitionTicks);o.putInt("AzharSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("AzharSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("AzharTransition",0),0,84);summoningTicks=Math.clamp(i.getIntOr("AzharSummoning",0),0,72);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(AzharCombat.REFRACTION)?2:a.equals(AzharCombat.STAMPEDE)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("dawnbronze").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.AZHAR_TROPHY.get()));}
}

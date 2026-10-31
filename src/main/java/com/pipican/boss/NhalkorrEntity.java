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

/** Northern leviathan with telegraphed currents, undertow and surface breaches. */
public final class NhalkorrEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(NhalkorrEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",NhalkorrCombat.CURRENT,NhalkorrCombat.UNDERTOW,NhalkorrCombat.BREACHES);
    private final NhalkorrCombat combat=new NhalkorrCombat(this);private int sequence,transitionTicks,summoningTicks;
    public NhalkorrEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"nhalkorr_leviathan",BossEvent.BossBarColor.BLUE,260);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("nhalkorr_leviathan");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,18).add(Attributes.MOVEMENT_SPEED,0.21).add(Attributes.FOLLOW_RANGE,50).add(Attributes.KNOCKBACK_RESISTANCE,0.98);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int nhalkorrAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=76;}
    @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,getX(),getY()+.35,getZ(),16,.8,.3,.8,.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=86;playSound(SoundEvents.CONDUIT_ACTIVATE,1.25f,.55f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.NAUTILUS,getX(),getY()+1,getZ(),18,.85,.65,.85,.025);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>32)id=NhalkorrCombat.CURRENT;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?.96:.78);}else getNavigation().stop();combat.tick(l);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(NhalkorrCombat.CURRENT)?2:active.equals(NhalkorrCombat.UNDERTOW)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("NhalkorrSequence",sequence);o.putInt("NhalkorrTransition",transitionTicks);o.putInt("NhalkorrSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("NhalkorrSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("NhalkorrTransition",0),0,86);summoningTicks=Math.clamp(i.getIntOr("NhalkorrSummoning",0),0,76);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(NhalkorrCombat.CURRENT)?2:a.equals(NhalkorrCombat.UNDERTOW)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("glacierite").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.NHALKORR_TROPHY.get()));}
}

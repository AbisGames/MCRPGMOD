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

/** Drowned jarl with returning oath and breakable chain battlefield control. */
public final class HrothgarEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(HrothgarEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",HrothgarCombat.OATH,HrothgarCombat.CHAINS,HrothgarCombat.ROWERS);
    private final HrothgarCombat combat=new HrothgarCombat(this);private int sequence,transitionTicks,summoningTicks;
    public HrothgarEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"hrothgar_drowned_jarl",BossEvent.BossBarColor.BLUE,250);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("hrothgar_drowned_jarl");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,16).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,.97);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);}public int hrothgarAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability a,LivingEntity t){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=72;}
    @Override protected void tickBossCombat(ServerLevel l){if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)l.sendParticles(ParticleTypes.SOUL,getX(),getY()+.5,getZ(),13,.7,.35,.7,.02);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=84;playSound(SoundEvents.RAVAGER_ROAR,1.25f,.62f);}if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+1,getZ(),16,.8,.7,.8,.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>32)id=HrothgarCombat.OATH;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,.82);}else getNavigation().stop();combat.tick(l);String active=combat.clock().active();entityData.set(ACTION,active.isEmpty()?0:active.equals("basic")?1:active.equals(HrothgarCombat.OATH)?2:active.equals(HrothgarCombat.CHAINS)?3:4);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);combat.save(o);o.putInt("HrothgarSequence",sequence);o.putInt("HrothgarTransition",transitionTicks);o.putInt("HrothgarSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);combat.load(i);sequence=Math.floorMod(i.getIntOr("HrothgarSequence",0),PATTERN.size());transitionTicks=Math.clamp(i.getIntOr("HrothgarTransition",0),0,84);summoningTicks=Math.clamp(i.getIntOr("HrothgarSummoning",0),0,72);String a=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:a.isEmpty()?0:a.equals("basic")?1:a.equals(HrothgarCombat.OATH)?2:a.equals(HrothgarCombat.CHAINS)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKilled){spawnAtLocation(l,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("stormsilver").get(),8));spawnAtLocation(l,new net.minecraft.world.item.ItemStack(BossContent.HROTHGAR_TROPHY.get()));}
}

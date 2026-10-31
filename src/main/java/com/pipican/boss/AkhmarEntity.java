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

/** Sand emperor with marked procession lanes, breakable sun obelisks and saved eruptions. */
public final class AkhmarEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(AkhmarEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",AkhmarCombat.PROCESSION,AkhmarCombat.OBELISKS,AkhmarCombat.COURT);
    private final AkhmarCombat combat=new AkhmarCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public AkhmarEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"akhmar_sand_emperor",BossEvent.BossBarColor.YELLOW,225);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("akhmar_sand_emperor");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,14).add(Attributes.MOVEMENT_SPEED,0.25)
        .add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.9);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int akhmarAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=64;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,getX(),getY()+0.2,getZ(),14,0.8,0.2,0.8,0.015);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=72;playSound(SoundEvents.SAND_PLACE,1.3f,0.55f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1.1,getZ(),12,0.9,0.7,0.9,0.02);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>30)id=AkhmarCombat.PROCESSION;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?1.02:0.86);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();int action=active.isEmpty()?0:active.equals("basic")?1:active.equals(AkhmarCombat.PROCESSION)?2:active.equals(AkhmarCombat.OBELISKS)?3:4;entityData.set(ACTION,action);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("AkhmarSequence",sequence);out.putInt("AkhmarTransition",transitionTicks);out.putInt("AkhmarSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("AkhmarSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("AkhmarTransition",0),0,72);summoningTicks=Math.clamp(in.getIntOr("AkhmarSummoning",0),0,64);String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(AkhmarCombat.PROCESSION)?2:active.equals(AkhmarCombat.OBELISKS)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("sunglass").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.AKHMAR_TROPHY.get()));}
}

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

/** Mire witch queen with persistent poison pools, marked cloud and decoy hex. */
public final class MorgathaEntity extends RegionalBossEntity {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(MorgathaEntity.class,EntityDataSerializers.INT);
    private static final List<String> PATTERN=List.of("basic",MorgathaCombat.BUBBLES,MorgathaCombat.VEIL,MorgathaCombat.DECOYS);
    private final MorgathaCombat combat=new MorgathaCombat(this);
    private int sequence,transitionTicks,summoningTicks;
    public MorgathaEntity(EntityType<? extends RegionalBossEntity> type,Level level){super(type,level,"morgatha_swamp_queen",BossEvent.BossBarColor.GREEN,150);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){var d=BossDesigns.require("morgatha_swamp_queen");return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage()).add(Attributes.ARMOR,10).add(Attributes.MOVEMENT_SPEED,0.23)
        .add(Attributes.FOLLOW_RANGE,44).add(Attributes.KNOCKBACK_RESISTANCE,0.72);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTION,0);}
    public int morgathaAction(){return entityData.get(ACTION);}
    @Override public boolean beginAbility(BossCombatState.Ability ability,LivingEntity target){return false;}
    @Override void activateEncounter(){super.activateEncounter();summoningTicks=55;}
    @Override protected void tickBossCombat(ServerLevel level){
        if(summoningTicks>0){summoningTicks--;getNavigation().stop();if(summoningTicks%5==0)level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,getX(),getY()+1,getZ(),12,0.8,0.55,0.8,0.025);entityData.set(ACTION,summoningTicks>0?6:0);return;}
        if(combat.updatePhase(getHealth(),getMaxHealth())){transitionTicks=68;playSound(SoundEvents.WITCH_CELEBRATE,1.25f,0.72f);}
        if(transitionTicks>0){getNavigation().stop();if(transitionTicks%5==0)level.sendParticles(ParticleTypes.WITCH,getX(),getY()+1.1,getZ(),16,0.9,0.65,0.9,0.03);transitionTicks--;entityData.set(ACTION,5);syncCombat(2,BossCombatState.Ability.PHASE_CHANGE,transitionTicks);return;}
        var target=getTarget();if(target!=null&&(!target.isAlive()||!insideArena(target.position()))){setTarget(null);target=null;}
        if(target!=null&&combat.clock().active().isEmpty()&&combat.clock().cooldown()==0&&hasLineOfSight(target)){String id=PATTERN.get(Math.floorMod(sequence,PATTERN.size()));if(id.equals("basic")&&distanceToSqr(target)>25)id=MorgathaCombat.BUBBLES;if(combat.begin(id,target))sequence=(sequence+1)%PATTERN.size();}
        if(combat.clock().active().isEmpty()){if(target!=null)getNavigation().moveTo(target,combat.clock().phase()==2?0.95:0.82);}else getNavigation().stop();
        combat.tick(level);String active=combat.clock().active();int action=active.isEmpty()?0:active.equals("basic")?1:active.equals(MorgathaCombat.BUBBLES)?2:active.equals(MorgathaCombat.VEIL)?3:4;entityData.set(ACTION,action);syncCombat(combat.clock().phase(),BossCombatState.Ability.NONE,combat.clock().remaining());
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);combat.save(out);out.putInt("MorgathaSequence",sequence);out.putInt("MorgathaTransition",transitionTicks);out.putInt("MorgathaSummoning",summoningTicks);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);combat.load(in);sequence=Math.floorMod(in.getIntOr("MorgathaSequence",0),PATTERN.size());transitionTicks=Math.clamp(in.getIntOr("MorgathaTransition",0),0,68);summoningTicks=Math.clamp(in.getIntOr("MorgathaSummoning",0),0,55);String active=combat.clock().active();entityData.set(ACTION,summoningTicks>0?6:transitionTicks>0?5:active.isEmpty()?0:active.equals("basic")?1:active.equals(MorgathaCombat.BUBBLES)?2:active.equals(MorgathaCombat.VEIL)?3:4);syncCombat(combat.clock().phase(),summoningTicks>0?BossCombatState.Ability.SUMMONING:transitionTicks>0?BossCombatState.Ability.PHASE_CHANGE:BossCombatState.Ability.NONE,summoningTicks>0?summoningTicks:transitionTicks>0?transitionTicks:combat.clock().remaining());}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKilled){spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("mirestone").get(),8));spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.MORGATHA_TROPHY.get()));}
}

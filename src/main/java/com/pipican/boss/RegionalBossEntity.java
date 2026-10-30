package com.pipican.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent server combat state and native multiplayer boss-bar tracking. */
public class RegionalBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(RegionalBossEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ABILITY=SynchedEntityData.defineId(RegionalBossEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REMAINING=SynchedEntityData.defineId(RegionalBossEntity.class,EntityDataSerializers.INT);
    private final BossCombatState combat=new BossCombatState();
    private final ServerBossEvent bar=new ServerBossEvent(java.util.UUID.randomUUID(),Component.translatable("entity.pipican.morvath_ashen_king"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
    public RegionalBossEntity(EntityType<? extends RegionalBossEntity> type,Level level){ super(type,level); this.xpReward=150; setPersistenceRequired(); }
    public static AttributeSupplier.Builder attributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,600).add(Attributes.ARMOR,12)
            .add(Attributes.ATTACK_DAMAGE,12).add(Attributes.MOVEMENT_SPEED,0.24)
            .add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder); builder.define(PHASE,1); builder.define(ABILITY,0); builder.define(REMAINING,0);
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new LookAtPlayerGoal(this,Player.class,40));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public int phase(){ return entityData.get(PHASE); }
    public BossCombatState.Ability ability(){ return BossCombatState.Ability.values()[entityData.get(ABILITY)]; }
    public int abilityTicksRemaining(){ return entityData.get(REMAINING); }
    @Override protected void customServerAiStep(ServerLevel level){
        super.customServerAiStep(level);
        combat.updatePhase(getHealth(),getMaxHealth());
        // Ability effects are added by the concrete encounter implementation.
        combat.tick();
        entityData.set(PHASE,combat.phase()); entityData.set(ABILITY,combat.ability().ordinal()); entityData.set(REMAINING,combat.remaining());
        bar.setProgress(Math.clamp(getHealth()/getMaxHealth(),0,1));
    }
    @Override public void startSeenByPlayer(ServerPlayer player){ super.startSeenByPlayer(player); bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player){ super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override public boolean removeWhenFarAway(double distance){ return false; }
    @Override protected void addAdditionalSaveData(ValueOutput output){
        super.addAdditionalSaveData(output);
        output.putInt("BossPhase",combat.phase()); output.putInt("BossAbility",combat.ability().ordinal());
        output.putInt("BossRemaining",combat.remaining()); output.putInt("BossCooldown",combat.cooldown()); output.putBoolean("BossResolved",combat.resolved());
    }
    @Override protected void readAdditionalSaveData(ValueInput input){
        super.readAdditionalSaveData(input);
        combat.restore(input.getIntOr("BossPhase",1),input.getIntOr("BossAbility",0),input.getIntOr("BossRemaining",0),input.getIntOr("BossCooldown",40),input.getBooleanOr("BossResolved",false));
        entityData.set(PHASE,combat.phase()); entityData.set(ABILITY,combat.ability().ordinal()); entityData.set(REMAINING,combat.remaining());
    }
}

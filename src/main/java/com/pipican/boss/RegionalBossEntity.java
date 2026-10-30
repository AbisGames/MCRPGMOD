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
    private boolean managedEncounter;
    private int abandonedTicks;
    private final MorvathCombat attacks=new MorvathCombat(this);
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
        attacks.tick(level,combat);
        if(combat.phase()==2 && tickCount%10==0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY()+2,getZ(),5,0.8,1.4,0.8,0.01);
        entityData.set(PHASE,combat.phase()); entityData.set(ABILITY,combat.ability().ordinal()); entityData.set(REMAINING,combat.remaining());
        bar.setProgress(Math.clamp(getHealth()/getMaxHealth(),0,1));
    }
    /** Cleanup also runs when vanilla AI is disabled; chunk unload is handled separately. */
    @Override public void tick(){
        if(managedEncounter && isAlive() && level() instanceof ServerLevel level){
            var encounters=BossEncounters.get(level);
            var entry=encounters.find(getUUID()).orElse(null);
            if(entry==null || entry.status()!=BossEncounters.Status.ACTIVE){ discard(); return; }
            if(position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(entry.arena()))>32*32){ discard(); return; }
            boolean occupied=!level.getPlayers(player->player.isAlive() && !player.isSpectator() && !player.isCreative()
                && player.blockPosition().distSqr(entry.arena())<=48*48).isEmpty();
            abandonedTicks=occupied ? 0 : abandonedTicks+1;
            if(abandonedTicks>=600){
                encounters.finish(getUUID(),BossEncounters.Status.CANCELLED);
                discard(); return;
            }
        }
        super.tick();
    }
    @Override public void onRemoval(RemovalReason reason){
        if(level() instanceof ServerLevel level){
            if(managedEncounter && (reason.shouldDestroy() || reason==RemovalReason.CHANGED_DIMENSION))
                BossEncounters.get(level).finish(getUUID(),BossEncounters.Status.CANCELLED);
            attacks.clear();
            bar.removeAllPlayers();
        }
        super.onRemoval(reason);
    }
    public boolean insideArena(net.minecraft.world.phys.Vec3 position){
        if(!managedEncounter) return position.distanceToSqr(position())<=40*40;
        return level() instanceof ServerLevel level && BossEncounters.get(level).find(getUUID())
            .filter(entry->entry.status()==BossEncounters.Status.ACTIVE)
            .map(entry->position.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(entry.arena()))<=24*24).orElse(false);
    }
    /** Server-only ability request; timing and cooldown remain authoritative. */
    public boolean beginAbility(BossCombatState.Ability ability,net.minecraft.world.entity.LivingEntity target){
        return level() instanceof ServerLevel && insideArena(target.position()) && attacks.begin(combat,ability,target);
    }
    public void tickCombat(ServerLevel level){ if(level!=level()) throw new IllegalArgumentException("Wrong combat dimension"); customServerAiStep(level); }
    void activateEncounter(){ managedEncounter=true; combat.summon(); }
    @Override protected void dropAllDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source){
        if(!managedEncounter || !BossEncounters.get(level).finish(getUUID(),BossEncounters.Status.DEFEATED)) return;
        attacks.clear();
        super.dropAllDeathLoot(level,source);
    }
    @Override protected boolean isAlwaysExperienceDropper(){ return managedEncounter; }
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled){
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.HEART.get()));
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(com.pipican.worldgen.RegionalOreRegistries.MATERIALS.get("cinderite").get(),8));
        if(Math.floorMod(getUUID().hashCode(),4)==0) spawnAtLocation(level,new net.minecraft.world.item.ItemStack(BossContent.INFERNIUM.get()));
    }
    @Override public void startSeenByPlayer(ServerPlayer player){ super.startSeenByPlayer(player); bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player){ super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override public boolean removeWhenFarAway(double distance){ return false; }
    @Override protected void addAdditionalSaveData(ValueOutput output){
        super.addAdditionalSaveData(output);
        attacks.save(output);
        output.putBoolean("ManagedEncounter",managedEncounter); output.putInt("BossAbandonedTicks",abandonedTicks);
        output.putInt("BossPhase",combat.phase()); output.putInt("BossAbility",combat.ability().ordinal());
        output.putInt("BossRemaining",combat.remaining()); output.putInt("BossCooldown",combat.cooldown()); output.putBoolean("BossResolved",combat.resolved());
    }
    @Override protected void readAdditionalSaveData(ValueInput input){
        super.readAdditionalSaveData(input);
        attacks.load(input);
        managedEncounter=input.getBooleanOr("ManagedEncounter",false); abandonedTicks=Math.clamp(input.getIntOr("BossAbandonedTicks",0),0,600);
        combat.restore(input.getIntOr("BossPhase",1),input.getIntOr("BossAbility",0),input.getIntOr("BossRemaining",0),input.getIntOr("BossCooldown",40),input.getBooleanOr("BossResolved",false));
        entityData.set(PHASE,combat.phase()); entityData.set(ABILITY,combat.ability().ordinal()); entityData.set(REMAINING,combat.remaining());
    }
}

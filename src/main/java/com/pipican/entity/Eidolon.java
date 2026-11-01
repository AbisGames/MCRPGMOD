package com.pipican.entity;

import com.pipican.Pipican;
import com.pipican.ability.Ability;
import com.pipican.ability.MobControl;
import com.pipican.core.PipicanRegistries;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Engineer's eidolon: a big construct that belongs to one player. It follows its owner, fights
 * whatever hurts them or whatever they hit, never targets players, sits on sneak + right-click, is
 * repaired with iron ingots and can be ridden (the Cockpit ability). Its owner's class can upgrade it
 * with {@code pipican:eidolon} effects, which it reads every couple of seconds.
 *
 * <p>Built with the {@code pipican:eidolon} ability action; when it is destroyed, that ability goes
 * on its rebuild cooldown.
 */
public class Eidolon extends IronGolem implements OwnableEntity {

    private static final Map<UUID, UUID> BY_OWNER = new ConcurrentHashMap<>();
    private static final Map<AttributeModifier.Operation, Identifier> OWNER_MODIFIERS = Map.of(
            AttributeModifier.Operation.ADD_VALUE, Pipican.id("eidolon/add_value"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Pipican.id("eidolon/add_multiplied_base"),
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Pipican.id("eidolon/add_multiplied_total"));

    private @Nullable EntityReference<LivingEntity> owner;
    private boolean sitting;
    private Optional<ResourceKey<Ability>> builtBy = Optional.empty();
    private long rebuildTicks;

    public Eidolon(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        setPlayerCreated(true);
    }

    public static AttributeSupplier.Builder createEidolonAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.SCALE, 1.3);
    }

    /** The eidolon of {@code player}, if it is loaded. */
    public static Optional<Eidolon> of(ServerPlayer player) {
        UUID id = BY_OWNER.get(player.getUUID());
        return id != null && player.level().getEntity(id) instanceof Eidolon e && e.isAlive() && e.isOwnedBy(player) ? Optional.of(e) : Optional.empty();
    }

    public void setOwner(ServerPlayer player, Optional<ResourceKey<Ability>> builtBy, long rebuildTicks) {
        this.owner = EntityReference.of(player);
        this.builtBy = builtBy;
        this.rebuildTicks = rebuildTicks;
        BY_OWNER.put(player.getUUID(), getUUID());
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return owner != null && owner.getUUID().equals(entity.getUUID());
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getOwnerReference() {
        return owner;
    }

    /** The owner if they are online and in this level (looked up through the player list). */
    private @Nullable ServerPlayer ownerPlayer() {
        if (owner == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getPlayerByUUID(owner.getUUID()) instanceof ServerPlayer p ? p : level.getServer().getPlayerList().getPlayer(owner.getUUID());
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        // No FloatGoal: it sinks (and doesn't drown, like every golem).
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        ServerPlayer player = ownerPlayer();
        return !(target instanceof Player) && !(owner != null && target.getUUID().equals(owner.getUUID()))
                && !(player != null && MobControl.isAllyOf(target, player)) && super.canAttack(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        ServerPlayer player = ownerPlayer();
        if (level().isClientSide() || player == null) {
            return;
        }
        BY_OWNER.put(player.getUUID(), getUUID());
        if (isVehicle()) {
            setTarget(null);
            getNavigation().stop();
        } else if (tickCount % 10 == 0 && !sitting) {
            // Defend the owner: whatever hurt them, else whatever they last hit.
            LivingEntity threat = player.getLastHurtByMob();
            if (threat == null || !threat.isAlive()) {
                threat = player.getLastHurtMob();
            }
            if (threat != null && threat.isAlive() && threat != this && canAttack(threat) && threat.distanceToSqr(this) < 32 * 32) {
                setTarget(threat);
            }
        }
        if (tickCount % 40 == 0) {
            applyOwnerUpgrades(player);
        }
    }

    /** Re-applies the owner's upgrades now (otherwise done every 2 seconds). */
    public void refreshUpgrades() {
        ServerPlayer player = ownerPlayer();
        if (player != null) {
            applyOwnerUpgrades(player);
        }
    }

    /** Applies the owner's {@code pipican:eidolon} upgrades as modifiers (rebuilt from scratch each time). */
    private void applyOwnerUpgrades(ServerPlayer player) {
        Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> upgrades = ClassEffectApplier.state(player).eidolonUpgrades();
        for (Holder<Attribute> attribute : new Holder[] {Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.SCALE, Attributes.ATTACK_KNOCKBACK}) {
            AttributeInstance instance = getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            OWNER_MODIFIERS.values().forEach(instance::removeModifier);
            upgrades.getOrDefault(attribute, Map.of()).forEach((op, amount) -> {
                if (amount != 0) {
                    instance.addTransientModifier(new AttributeModifier(OWNER_MODIFIERS.get(op), amount, op));
                }
            });
        }
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = ownerPlayer();
            return !sitting && !isVehicle() && o != null && getTarget() == null && distanceToSqr(o) > 12 * 12;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity o = ownerPlayer();
            return !sitting && o != null && getTarget() == null && distanceToSqr(o) > 4 * 4;
        }

        @Override
        public void tick() {
            LivingEntity o = ownerPlayer();
            if (o == null) {
                return;
            }
            if (distanceToSqr(o) > 32 * 32) {
                snapTo(o.getX(), o.getY(), o.getZ(), getYRot(), getXRot());
                getNavigation().stop();
            } else if (tickCount % 10 == 0) {
                getNavigation().moveTo(o, 1.2);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ riding (Cockpit)

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p && isOwnedBy(p) ? p : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rotation = new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
        setRot(rotation.y, rotation.x);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float forward = controller.zza <= 0 ? controller.zza * 0.25F : controller.zza;
        return new Vec3(controller.xxa * 0.5F, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // ------------------------------------------------------------------ immunities, damage, death

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        var e = effect.getEffect();
        if (e.is(MobEffects.POISON) || e.is(MobEffects.WITHER) || e.is(MobEffects.NAUSEA) || e.is(MobEffects.BLINDNESS) || e.is(MobEffects.DARKNESS)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return fallDistance >= 10 && super.causeFallDamage(fallDistance, damageModifier, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // The owner can't hurt their own eidolon by accident.
        if (source.getEntity() != null && owner != null && source.getEntity().getUUID().equals(owner.getUUID())) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // Drops nothing: it has to be rebuilt.
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ServerPlayer player = ownerPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("pipican.eidolon.destroyed", rebuildTicks / 20 / 60));
            builtBy.ifPresent(key -> {
                ClassService.data(player).abilityCooldowns().put(key, level().getGameTime() + rebuildTicks);
                ClassService.changed(player);
            });
            BY_OWNER.remove(player.getUUID(), getUUID());
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            // Spare Parts: 10 HP, plus "Spare Parts heal 15 HP"-style upgrades (pipican:reaction_mod "spare parts").
            double extra = player instanceof net.minecraft.server.level.ServerPlayer sp
                    ? com.pipican.player.ClassEffectApplier.state(sp).reactionMods().getOrDefault("spare parts", com.pipican.player.ClassState.ReactionMod.NONE).heal() : 0;
            heal((float) (10 + extra));
            stack.consume(1, player);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.pipican.event.MasteryEvents.did(sp, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.EIDOLON_REPAIR,
                        stack, java.util.Optional.empty(), java.util.Optional.of(this), 10 + extra));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            sitting = !sitting;
            getNavigation().stop();
            setTarget(null);
            player.sendOverlayMessage(Component.translatable(sitting ? "pipican.eidolon.sit" : "pipican.eidolon.follow"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        EntityReference.store(owner, output, "Owner");
        output.putBoolean("Sitting", sitting);
        builtBy.ifPresent(key -> output.putString("BuiltBy", key.identifier().toString()));
        output.putLong("RebuildTicks", rebuildTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        owner = EntityReference.read(input, "Owner");
        sitting = input.getBooleanOr("Sitting", false);
        builtBy = input.getString("BuiltBy").map(Identifier::tryParse).map(id -> ResourceKey.create(PipicanRegistries.ABILITY, id));
        rebuildTicks = input.getLongOr("RebuildTicks", 8000L);
    }
}

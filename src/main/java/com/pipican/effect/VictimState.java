package com.pipican.effect;

import com.pipican.ability.Charged;
import com.pipican.ability.Bleeds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Marks;
import com.pipican.condition.Bounds;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * What a damage bonus requires of the mob being hit ({@code "victim"} on {@code pipican:damage_dealt}):
 * <pre>{@code "victim": { "health": { "max": 0.5 }, "harmful_effect": true, "not_targeting_you": true, "marked": true, "on_fire": true, "bleeding": true, "charged_hit": true } }</pre>
 * Every field is optional; all given ones must hold. {@code health} is health / max health (0-1);
 * {@code marked} means marked by the attacker's {@code pipican:mark} abilities.
 */
public record VictimState(Optional<Bounds> health, boolean harmfulEffect, boolean notTargetingYou, boolean marked, boolean onFire, boolean bleeding, boolean chargedHit, boolean unarmored,
                          Optional<HolderSet<MobEffect>> effects, Optional<Double> within, boolean targetingOther, boolean upperHit, boolean fromBehind) {

    public static final Codec<VictimState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Bounds.MAP_CODEC.codec().optionalFieldOf("health").forGetter(VictimState::health),
            Codec.BOOL.optionalFieldOf("harmful_effect", false).forGetter(VictimState::harmfulEffect),
            Codec.BOOL.optionalFieldOf("not_targeting_you", false).forGetter(VictimState::notTargetingYou),
            Codec.BOOL.optionalFieldOf("marked", false).forGetter(VictimState::marked),
            Codec.BOOL.optionalFieldOf("on_fire", false).forGetter(VictimState::onFire),
            Codec.BOOL.optionalFieldOf("bleeding", false).forGetter(VictimState::bleeding),
            Codec.BOOL.optionalFieldOf("charged_hit", false).forGetter(VictimState::chargedHit),
            Codec.BOOL.optionalFieldOf("unarmored", false).forGetter(VictimState::unarmored),
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).optionalFieldOf("effects").forGetter(VictimState::effects),
            Codec.DOUBLE.optionalFieldOf("within").forGetter(VictimState::within),
            Codec.BOOL.optionalFieldOf("targeting_other", false).forGetter(VictimState::targetingOther),
            Codec.BOOL.optionalFieldOf("upper_hit", false).forGetter(VictimState::upperHit),
            Codec.BOOL.optionalFieldOf("from_behind", false).forGetter(VictimState::fromBehind)
    ).apply(i, VictimState::new));

    /**
     * {@code upper_hit}: the hit lands on the upper part of the mob (a projectile above 70% of its height, or a
     * melee attacker whose eyes are that high).
     */
    public boolean test(net.minecraft.world.damagesource.DamageSource source, LivingEntity attacker, LivingEntity victim) {
        if (upperHit) {
            var direct = source.getDirectEntity();
            double y = direct == null ? attacker.getEyeY() : direct == attacker ? attacker.getEyeY() : direct.getY();
            if (y < victim.getY() + victim.getBbHeight() * 0.7) {
                return false;
            }
        }
        return test(attacker, victim);
    }

    /** {@code from_behind}: the attacker is more than 90 degrees away from where the victim faces. */
    public static boolean behind(LivingEntity attacker, LivingEntity victim) {
        net.minecraft.world.phys.Vec3 facing = net.minecraft.world.phys.Vec3.directionFromRotation(0, victim.getYHeadRot());
        net.minecraft.world.phys.Vec3 to = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
        return to.lengthSqr() > 1.0e-4 && facing.dot(to.normalize()) < 0;
    }

    public boolean test(LivingEntity attacker, LivingEntity victim) {
        if (health.isPresent() && !(victim.getMaxHealth() > 0 && health.get().test(victim.getHealth() / victim.getMaxHealth()))) {
            return false;
        }
        if (harmfulEffect && victim.getActiveEffects().stream().noneMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)) {
            return false;
        }
        if (notTargetingYou && victim instanceof Mob mob && mob.getTarget() == attacker) {
            return false;
        }
        if (marked && !(attacker instanceof Player p && Marks.isMarkedBy(victim, p))) {
            return false;
        }
        if (chargedHit && !(attacker instanceof net.minecraft.server.level.ServerPlayer p && Charged.wasCharged(p))) {
            return false;
        }
        if (effects.isPresent() && effects.get().stream().noneMatch(victim::hasEffect)) {
            return false;
        }
        if (within.isPresent() && attacker.distanceTo(victim) > within.get()) {
            return false;
        }
        if (targetingOther && !(victim instanceof Mob mob && mob.getTarget() != null && mob.getTarget() != attacker)) {
            return false;
        }
        if (fromBehind && !behind(attacker, victim)) {
            return false;
        }
        if (unarmored && victim.getArmorValue() > 0) {
            return false;
        }
        if (bleeding && !Bleeds.isBleeding(victim)) {
            return false;
        }
        return !onFire || victim.isOnFire();
    }
}

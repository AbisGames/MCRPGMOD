package com.pipican.ability;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * What an {@link AbilityAction} runs with.
 *
 * @param player  who is acting
 * @param ability the ability being used, if any (reactions from {@code pipican:on} have none)
 * @param origin  the centre for area targets: the player, an anchored state's spot, or the event's mob
 * @param subject the other entity of a reaction (the mob hit, killed, or the attacker), if any
 * @param spent   resource spent on this use (for {@code spend_all} abilities)
 */
public final class AbilityContext {

    private final ServerPlayer player;
    private final Optional<ResourceKey<Ability>> ability;
    private final Vec3 origin;
    private final Optional<LivingEntity> subject;
    private final double spent;
    private Component cancelled;
    /** Upgrades of the reaction running these actions ({@code pipican:reaction_mod}); NONE otherwise. */
    private com.pipican.player.ClassState.ReactionMod mod = com.pipican.player.ClassState.ReactionMod.NONE;

    public AbilityContext(ServerPlayer player, Optional<ResourceKey<Ability>> ability, Vec3 origin, Optional<LivingEntity> subject, double spent) {
        this.player = player;
        this.ability = ability;
        this.origin = origin;
        this.subject = subject;
        this.spent = spent;
    }

    public static AbilityContext of(ServerPlayer player, ResourceKey<Ability> ability, double spent) {
        return new AbilityContext(player, Optional.of(ability), player.position(), Optional.empty(), spent);
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return player.level();
    }

    public Optional<ResourceKey<Ability>> ability() {
        return ability;
    }

    public Vec3 origin() {
        return origin;
    }

    public Optional<LivingEntity> subject() {
        return subject;
    }

    public com.pipican.player.ClassState.ReactionMod mod() {
        return mod;
    }

    public AbilityContext withMod(com.pipican.player.ClassState.ReactionMod mod) {
        this.mod = mod;
        return this;
    }

    public double spent() {
        return spent;
    }

    /**
     * Stops the use: later actions don't run, the cost is refunded and no cooldown starts. For
     * actions that need something that isn't there (no target in sight, no waypoint saved...).
     */
    public void cancel(Component reason) {
        if (cancelled == null) {
            cancelled = reason;
        }
    }

    public boolean isCancelled() {
        return cancelled != null;
    }

    public Component cancelReason() {
        return cancelled;
    }
}

package com.pipican.ability;

import com.pipican.player.ClassState;

import net.minecraft.world.item.ItemStack;
import com.pipican.event.MasteryEvents;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.effect.type.BonusEffect;
import com.pipican.condition.ClassCondition;
import com.pipican.core.ClassLookup;
import com.pipican.mastery.SkillAction;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.MasteryService;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Using abilities, and ticking the ones that are running. Server side only; the ability keys send
 * a slot number and everything is validated here.
 *
 * <p>Cooldowns, running states, per-rest uses and spell points are saved on the player
 * ({@link PlayerClassData}), so relogging doesn't reset them and the HUD can read them.
 */
public final class AbilityService {

    /** Where anchored states were used (not saved: after a restart they pulse around the player). */
    private static final Map<UUID, Map<ResourceKey<Ability>, Vec3>> ANCHORS = new ConcurrentHashMap<>();

    private AbilityService() {}

    /** Uses the ability on key {@code slot} (0-3). Failures are explained in the action bar. */
    public static void useSlot(ServerPlayer player, int slot) {
        PlayerClassData data = ClassService.data(player);
        List<ResourceKey<Ability>> slots = AbilitySlots.slots(player.level().registryAccess(), data);
        if (slot < 0 || slot >= slots.size()) {
            return;
        }
        ClassService.Result result = use(player, slots.get(slot));
        if (!result.success()) {
            player.sendOverlayMessage(result.message().copy().withStyle(ChatFormatting.RED));
        }
    }

    /** Uses an ability, or stops it if it is a running toggle. */
    public static ClassService.Result use(ServerPlayer player, ResourceKey<Ability> key) {
        PlayerClassData data = ClassService.data(player);
        var access = player.level().registryAccess();
        if (!AbilitySlots.granted(access, data).contains(key)) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.not_granted"));
        }
        Ability ability = ClassLookup.abilities(access).getValue(key);
        if (data.abilityActive().containsKey(key)) {
            if (ability.toggle()) {
                if (ClassEffectApplier.state(player).abilityMod(key).locked()) {
                    return ClassService.Result.fail(Component.translatable("pipican.ability.locked", ability.name()));
                }
                stop(player, key, ability);
                return ClassService.Result.ok(Component.translatable("pipican.ability.stopped", ability.name()));
            }
            return ClassService.Result.fail(Component.translatable("pipican.ability.already_active", ability.name()));
        }
        long now = player.level().getGameTime();
        long readyAt = data.abilityReadyAt(key);
        if (now < readyAt) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.cooldown", ability.name(), (readyAt - now + 19) / 20));
        }
        if (ability.usesPerRest() > 0 && data.extras().restUses.getOrDefault(key, 0) >= ability.usesPerRest() + ClassEffectApplier.state(player).abilityMod(key).uses()) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.needs_rest", ability.name()));
        }
        if (ability.requires().isPresent() && !ability.requires().get().test(player)) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.cannot_use", ability.name()));
        }
        boolean spellPoint = usesSpellPoint(player, ability);
        double cost = cost(player, ability);
        if (!spellPoint && cost > 0 && data.resource() < cost) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.no_resource", ability.name(), (int) Math.ceil(cost)));
        }
        AbilityContext ctx = activate(player, key, ability, spellPoint);
        if (ctx.isCancelled()) {
            return ClassService.Result.fail(ctx.cancelReason());
        }
        return ClassService.Result.ok(Component.translatable("pipican.ability.used", ability.name()));
    }

    /** The resource an ability costs this player, after {@code ability_cost} bonuses. */
    public static double cost(ServerPlayer player, Ability ability) {
        var state = ClassEffectApplier.state(player);
        return ability.cost() * state.bonus(BonusEffect.Kind.ABILITY_COST) * Math.max(0, 1 + state.abilityMod(key(player, ability)).cost());
    }

    /** The registry key of {@code ability} (abilities are looked up by value in a few places). */
    private static ResourceKey<Ability> key(ServerPlayer player, Ability ability) {
        return ClassLookup.abilities(player.level().registryAccess()).getResourceKey(ability).orElse(null);
    }

    /** An ability's cooldown for this player, after {@code ability_cooldown} bonuses. */
    public static long cooldown(ServerPlayer player, Ability ability) {
        var state = ClassEffectApplier.state(player);
        return Math.max(0, Math.round(ability.cooldownTicks() * state.bonus(BonusEffect.Kind.ABILITY_COOLDOWN)
                + state.abilityMod(key(player, ability)).cooldownSeconds() * 20));
    }

    /** Spell points (Wizard) pay for abilities that cost resource, while there are any left. */
    private static boolean usesSpellPoint(ServerPlayer player, Ability ability) {
        return ability.cost() > 0 && !ability.spendAll() && ClassEffectApplier.state(player).spellPoints() > 0
                && ClassService.data(player).extras().spellPoints > 0;
    }

    /**
     * Admin/testing: uses an ability even if the player doesn't have it, ignoring cooldown, cost,
     * {@code requires} and per-rest limits. A running state is restarted. Returns the context (to see
     * whether an action cancelled it).
     */
    public static AbilityContext force(ServerPlayer player, ResourceKey<Ability> key) {
        Ability ability = ClassLookup.abilities(player.level().registryAccess()).getValue(key);
        PlayerClassData data = ClassService.data(player);
        data.abilityActive().remove(key);
        double resource = data.resource();
        data.setResource(Math.max(resource, ability.cost()));
        AbilityContext ctx = activate(player, key, ability, false);
        data.setResource(resource);
        data.extras().restUses.remove(key);
        return ctx;
    }

    /** Admin: clears every cooldown, per-rest use and running state. */
    public static void reset(ServerPlayer player) {
        ClassService.data(player).abilityCooldowns().clear();
        ClassService.data(player).extras().restUses.clear();
        clearActive(player);
        player.syncData(PipicanAttachments.CLASS_DATA);
    }

    /** Abilities used since login, for {@code free_ability_every}. */
    private static final Map<java.util.UUID, Integer> USES = new ConcurrentHashMap<>();

    private static AbilityContext activate(ServerPlayer player, ResourceKey<Ability> key, Ability ability, boolean spellPoint) {
        PlayerClassData data = ClassService.data(player);
        long now = player.level().getGameTime();
        int every = (int) Math.round(ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.FREE_ABILITY_EVERY));
        boolean free = every > 1 && (USES.getOrDefault(player.getUUID(), 0) + 1) % every == 0;
        double spent = spellPoint || free ? 0 : ability.spendAll() ? data.resource() : Math.min(cost(player, ability), data.resource());
        data.setResource(data.resource() - spent);
        if (spellPoint) {
            data.extras().spellPoints--;
        }
        var upgrade = ClassEffectApplier.state(player).abilityMod(key);
        AbilityContext ctx = AbilityContext.of(player, key, spent)
                .withMod(new ClassState.ReactionMod(upgrade.seconds(), 0, 0, 0, List.of(), 0, upgrade.amplify()));
        for (AbilityAction action : java.util.stream.Stream.concat(ability.actions().stream(), upgrade.actions().stream()).toList()) {
            action.run(ctx);
            if (ctx.isCancelled()) {
                // Nothing happened (no target, no waypoint...): refund and don't start a cooldown.
                data.setResource(Math.min(data.resourceMax(), data.resource() + spent));
                if (spellPoint) {
                    data.extras().spellPoints++;
                }
                player.syncData(PipicanAttachments.CLASS_DATA);
                return ctx;
            }
        }
        USES.merge(player.getUUID(), 1, Integer::sum);
        ClassState refundState = ClassEffectApplier.state(player);
        if (spent > 0 && player.getRandom().nextDouble() < refundState.chance(com.pipican.effect.type.BonusEffect.Kind.RESOURCE_REFUND)) {
            data.setResource(Math.min(data.resourceMax(), data.resource() + spent * refundState.bonus(com.pipican.effect.type.BonusEffect.Kind.REFUND_BONUS)));
        }
        if (ability.toggle()) {
            data.abilityActive().put(key, Long.MAX_VALUE);
        } else {
            if (ability.durationTicks() > 0) {
                double longer = Math.max(0, 1 + ClassEffectApplier.state(player).abilityMod(key).duration());
                data.abilityActive().put(key, now + Math.round(ability.durationTicks() * longer));
            }
            data.abilityCooldowns().put(key, now + cooldown(player, ability));
        }
        if (ability.timing().anchored()) {
            ANCHORS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(key, player.position());
        }
        if (ability.usesPerRest() > 0) {
            data.extras().restUses.merge(key, 1, Integer::sum);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.3f);
        MasteryService.onAction(player, new SkillAction.AbilityUsed(key.identifier(), player.getMainHandItem()));
        if (spent > 0) {
            Reactions.fire(player, null, player.getMainHandItem(), ReactionEffect.Event.SPEND);
            MasteryEvents.did(player, new SkillAction.Did(SkillAction.Did.Kind.SPEND, ItemStack.EMPTY, Optional.empty(), Optional.empty(), spent));
        }
        // Refresh so "while active" effects and ability_active conditions take effect; also syncs.
        ClassService.changed(player);
        return ctx;
    }

    /** Ends {@code key} if it is running (with its end actions). */
    public static void stopIfActive(ServerPlayer player, ResourceKey<Ability> key) {
        if (ClassService.data(player).abilityActive().containsKey(key)) {
            Ability ability = ClassLookup.abilities(player.level().registryAccess()).getValue(key);
            if (ability != null) {
                stop(player, key, ability);
            }
        }
    }

    private static void stop(ServerPlayer player, ResourceKey<Ability> key, Ability ability) {
        PlayerClassData data = ClassService.data(player);
        data.abilityActive().remove(key);
        AbilityContext ctx = new AbilityContext(player, Optional.of(key), anchor(player, key), Optional.empty(), 0);
        for (AbilityAction action : ability.endActions()) {
            action.run(ctx);
        }
        var anchors = ANCHORS.get(player.getUUID());
        if (anchors != null) {
            anchors.remove(key);
        }
        if (ability.toggle()) {
            data.abilityCooldowns().put(key, player.level().getGameTime() + cooldown(player, ability));
        }
        ClassService.changed(player);
    }

    private static Vec3 anchor(ServerPlayer player, ResourceKey<Ability> key) {
        var anchors = ANCHORS.get(player.getUUID());
        Vec3 at = anchors == null ? null : anchors.get(key);
        return at != null ? at : player.position();
    }

    /** Ends every running state without its end actions (death, class change). */
    public static void clearActive(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        ANCHORS.remove(player.getUUID());
        if (!data.abilityActive().isEmpty()) {
            data.abilityActive().clear();
            ClassService.changed(player);
        }
    }

    /** After sleeping: per-rest uses come back, spell points refill, temporary items vanish, Optimization is chosen again. */
    public static void onWake(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (!data.hasClass()) {
            return;
        }
        data.extras().restUses.clear();
        Boosts.onWake(player);
        Puppets.onWake(player);
        data.extras().spellPoints = ClassEffectApplier.state(player).spellPoints();
        TemporaryItems.removeAll(player);
        if (ClassEffectApplier.state(player).optimizationAmount() > 0) {
            Optimize.prompt(player);
        }
        if (ClassEffectApplier.state(player).favoredWeapon()) {
            FavoredWeapon.choose(player);
        }
        Reactions.rest(player);
        Reactions.fire(player, null, player.getMainHandItem(), ReactionEffect.Event.WAKE);
        ClassService.changed(player);
    }

    /** Every tick: drains toggles, pulses, ends expired states, starts {@code auto} abilities (every 10 ticks). */
    public static void tick(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (!data.hasClass()) {
            return;
        }
        var registry = ClassLookup.abilities(player.level().registryAccess());
        long now = player.level().getGameTime();
        List<ResourceKey<Ability>> ending = new ArrayList<>();
        for (Map.Entry<ResourceKey<Ability>, Long> e : List.copyOf(data.abilityActive().entrySet())) {
            Ability ability = registry.getValue(e.getKey());
            if (ability == null) {
                ending.add(e.getKey());
                continue;
            }
            if (ability.toggle() && ability.drainPerSecond() > 0) {
                double drain = ability.drainPerSecond() * Math.max(0, 1 + ClassEffectApplier.state(player).abilityMod(e.getKey()).drain());
                data.setResource(Math.max(0, data.resource() - drain / 20));
                if (data.resource() <= 0) {
                    ending.add(e.getKey());
                    continue;
                }
                if (player.tickCount % 10 == 0) {
                    player.syncData(PipicanAttachments.CLASS_DATA);
                }
            }
            int pulse = ability.timing().pulseTicks();
            if (pulse > 0 && now % pulse == 0) {
                AbilityContext ctx = new AbilityContext(player, Optional.of(e.getKey()), anchor(player, e.getKey()), Optional.empty(), 0);
                for (AbilityAction action : ability.pulseActions()) {
                    action.run(ctx);
                }
            }
            if (now >= e.getValue() || player.tickCount % 10 == 0 && ability.endWhen().map(c -> c.test(player)).orElse(false)) {
                ending.add(e.getKey());
            }
        }
        for (ResourceKey<Ability> key : ending) {
            Ability ability = registry.getValue(key);
            if (ability == null) {
                data.abilityActive().remove(key);
            } else {
                stop(player, key, ability);
            }
        }
        if (player.tickCount % 10 == 0) {
            for (ResourceKey<Ability> key : AbilitySlots.granted(player.level().registryAccess(), data)) {
                Ability ability = registry.getValue(key);
                Optional<ClassCondition> auto = ability.auto();
                if (auto.isPresent() && !data.abilityActive().containsKey(key) && now >= data.abilityReadyAt(key)
                        && data.resource() >= cost(player, ability) && auto.get().test(player)) {
                    activate(player, key, ability, false);
                }
            }
        }
        if (player.tickCount % 40 == 0) {
            TemporaryItems.removeExpired(player);
        }
    }
}

package com.pipican.ability;

import com.pipican.condition.ClassCondition;
import com.pipican.core.ClassLookup;
import com.pipican.mastery.SkillAction;
import com.pipican.player.ClassService;
import com.pipican.player.MasteryService;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Using abilities, and ticking the ones that are running. Server side only; the ability keys send
 * a slot number and everything is validated here.
 *
 * <p>Cooldowns and running states are saved on the player ({@link PlayerClassData}) as game-time
 * stamps, so relogging doesn't reset them and the HUD can read them from the synced data.
 */
public final class AbilityService {

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
        if (ability.requires().isPresent() && !ability.requires().get().test(player)) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.cannot_use", ability.name()));
        }
        if (ability.cost() > 0 && data.resource() < ability.cost()) {
            return ClassService.Result.fail(Component.translatable("pipican.ability.no_resource", ability.name(), (int) Math.ceil(ability.cost())));
        }
        activate(player, key, ability);
        return ClassService.Result.ok(Component.translatable("pipican.ability.used", ability.name()));
    }

    /**
     * Admin/testing: uses an ability even if the player doesn't have it, ignoring cooldown, cost and
     * {@code requires}. A running state is restarted.
     */
    public static void force(ServerPlayer player, ResourceKey<Ability> key) {
        Ability ability = ClassLookup.abilities(player.level().registryAccess()).getValue(key);
        if (ability == null) {
            return;
        }
        ClassService.data(player).abilityActive().remove(key);
        double resource = ClassService.data(player).resource();
        activate(player, key, ability);
        ClassService.data(player).setResource(resource);
    }

    /** Admin: clears every cooldown and running state. */
    public static void reset(ServerPlayer player) {
        ClassService.data(player).abilityCooldowns().clear();
        clearActive(player);
        player.syncData(PipicanAttachments.CLASS_DATA);
    }

    private static void activate(ServerPlayer player, ResourceKey<Ability> key, Ability ability) {
        PlayerClassData data = ClassService.data(player);
        long now = player.level().getGameTime();
        data.setResource(Math.max(0, data.resource() - ability.cost()));
        AbilityContext ctx = new AbilityContext(player, key, ability);
        for (AbilityAction action : ability.actions()) {
            action.run(ctx);
        }
        if (ability.toggle()) {
            data.abilityActive().put(key, Long.MAX_VALUE);
        } else {
            if (ability.durationTicks() > 0) {
                data.abilityActive().put(key, now + ability.durationTicks());
            }
            data.abilityCooldowns().put(key, now + ability.cooldownTicks());
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.3f);
        MasteryService.onAction(player, new SkillAction.AbilityUsed(key.identifier(), player.getMainHandItem()));
        // Refresh so "while active" effects and ability_active conditions take effect; also syncs.
        ClassService.changed(player);
    }

    private static void stop(ServerPlayer player, ResourceKey<Ability> key, Ability ability) {
        PlayerClassData data = ClassService.data(player);
        data.abilityActive().remove(key);
        AbilityContext ctx = new AbilityContext(player, key, ability);
        for (AbilityAction action : ability.endActions()) {
            action.run(ctx);
        }
        if (ability.toggle()) {
            data.abilityCooldowns().put(key, player.level().getGameTime() + ability.cooldownTicks());
        }
        ClassService.changed(player);
    }

    /** Ends every running state without its end actions (death, class change). */
    public static void clearActive(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (!data.abilityActive().isEmpty()) {
            data.abilityActive().clear();
            ClassService.changed(player);
        }
    }

    /** Every tick: drains toggles, ends expired states, starts {@code auto} abilities (every 10 ticks). */
    public static void tick(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (!data.hasClass()) {
            return;
        }
        var registry = ClassLookup.abilities(player.level().registryAccess());
        long now = player.level().getGameTime();
        List<ResourceKey<Ability>> ending = new ArrayList<>();
        for (Map.Entry<ResourceKey<Ability>, Long> e : data.abilityActive().entrySet()) {
            Ability ability = registry.getValue(e.getKey());
            if (ability == null) {
                ending.add(e.getKey());
                continue;
            }
            if (ability.toggle() && ability.drainPerSecond() > 0) {
                data.setResource(Math.max(0, data.resource() - ability.drainPerSecond() / 20));
                if (data.resource() <= 0) {
                    ending.add(e.getKey());
                    continue;
                }
                if (player.tickCount % 10 == 0) {
                    player.syncData(PipicanAttachments.CLASS_DATA);
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
                        && data.resource() >= ability.cost() && auto.get().test(player)) {
                    activate(player, key, ability);
                }
            }
        }
    }
}

package com.pipican.ability;

import com.pipican.core.ClassLookup;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.PlayerClassData;
import com.pipican.stat.StatDefinition;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Transmuter's Optimization choice. After sleeping (or using Alter Self) the player owes a choice:
 * a chat message with a clickable button for every pair of stats runs {@code /pipican optimize <raise> <lower>}.
 */
public final class Optimize {

    private Optimize() {}

    public static void prompt(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        data.extras().optimization = Optional.of(new PlayerClassData.Optimization(
                data.extras().optimization.flatMap(PlayerClassData.Optimization::raise),
                data.extras().optimization.flatMap(PlayerClassData.Optimization::lower), true));
        var stats = ClassLookup.stats(player.level().registryAccess());
        int amount = (int) ClassEffectApplier.state(player).optimizationAmount();
        player.sendSystemMessage(Component.translatable("pipican.optimize.prompt", amount).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (var raise : stats.entrySet()) {
            if (raise.getValue().exclusive()) {
                continue;
            }
            MutableComponent line = Component.literal(" +").append(raise.getValue().name()).append(": ").withStyle(ChatFormatting.GREEN);
            for (var lower : stats.entrySet()) {
                if (lower.getValue().exclusive() || lower.getKey().equals(raise.getKey())) {
                    continue;
                }
                String command = "/pipican optimize " + raise.getKey().identifier() + " " + lower.getKey().identifier();
                line.append(Component.literal("[-").append(lower.getValue().name()).append("] ")
                        .withStyle(s -> s.withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent.RunCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command)))));
            }
            player.sendSystemMessage(line);
        }
    }

    /** Applies a choice (from the clickable prompt). Only while a choice is owed. */
    public static ClassService.Result choose(ServerPlayer player, ResourceKey<StatDefinition> raise, ResourceKey<StatDefinition> lower) {
        PlayerClassData data = ClassService.data(player);
        if (ClassEffectApplier.state(player).optimizationAmount() <= 0) {
            return ClassService.Result.fail(Component.translatable("pipican.optimize.not_yours"));
        }
        if (data.extras().optimization.map(o -> !o.pending()).orElse(true)) {
            return ClassService.Result.fail(Component.translatable("pipican.optimize.not_pending"));
        }
        if (raise.equals(lower)) {
            return ClassService.Result.fail(Component.translatable("pipican.optimize.same"));
        }
        data.extras().optimization = Optional.of(new PlayerClassData.Optimization(Optional.of(raise), Optional.of(lower), false));
        ClassService.changed(player);
        return ClassService.Result.ok(Component.translatable("pipican.optimize.done"));
    }

    /** Optimization is lost on death (Transmuter weakness); a new choice is owed. */
    public static void onDeath(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (data.extras().optimization.isPresent()) {
            data.extras().optimization = Optional.of(new PlayerClassData.Optimization(Optional.empty(), Optional.empty(), true));
        }
    }
}

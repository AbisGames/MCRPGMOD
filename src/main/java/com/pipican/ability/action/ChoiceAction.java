package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;

/**
 * {@code pipican:choice} - the player picks one of several {@code options} and the ability runs the picked one ("choose
 * a curse and apply it"). Using the ability while sneaking switches to the next option (free, no cooldown); using it
 * normally runs the current one. The pick is remembered per player and ability until they log out.
 *
 * <pre>{@code { "type": "pipican:choice", "options": [
 *     { "name": "Desire", "actions": [ ... ] }, { "name": "Pain", "actions": [ ... ] } ] } }</pre>
 */
public record ChoiceAction(List<Option> options) implements AbilityAction {

    public record Option(String name, List<AbilityAction> actions) {
        // Lazy: actions contain actions.
        static final Codec<Option> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Option::name),
                Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().fieldOf("actions").forGetter(Option::actions)
        ).apply(i, Option::new));
    }

    public static final MapCodec<ChoiceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Option.CODEC.listOf().fieldOf("options").forGetter(ChoiceAction::options)
    ).apply(i, ChoiceAction::new));

    /** Player -> choice action -> picked option index. */
    private static final Map<UUID, Map<ChoiceAction, Integer>> PICKED = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CHOICE;
    }

    public static int picked(UUID player, ChoiceAction action) {
        return PICKED.getOrDefault(player, Map.of()).getOrDefault(action, 0);
    }

    @Override
    public void run(AbilityContext ctx) {
        if (options.isEmpty()) {
            return;
        }
        var player = ctx.player();
        Map<ChoiceAction, Integer> mine = PICKED.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        int index = Math.floorMod(mine.getOrDefault(this, 0), options.size());
        if (player.isShiftKeyDown()) {
            index = (index + 1) % options.size();
            mine.put(this, index);
            // Switching is free: the use is called off (refunded, no cooldown) with the new pick as the message.
            ctx.cancel(Component.translatable("pipican.ability.chosen", options.get(index).name()));
            return;
        }
        for (AbilityAction action : options.get(index).actions()) {
            action.run(ctx);
            if (ctx.isCancelled()) {
                return;
            }
        }
    }

    public static void forget(UUID player) {
        PICKED.remove(player);
    }
}

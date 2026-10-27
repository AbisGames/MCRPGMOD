package com.pipican.ability;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** What an {@link AbilityAction} runs with: who used which ability. */
public record AbilityContext(ServerPlayer player, ResourceKey<Ability> key, Ability ability) {

    public ServerLevel level() {
        return player.level();
    }
}

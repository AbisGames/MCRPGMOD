package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/**
 * {@code pipican:standing_on} - the block under the player's feet is one of these (an id, a list, or a {@code #tag}).
 *
 * <pre>{@code { "type": "pipican:standing_on", "blocks": "#minecraft:base_stone_overworld" } }</pre>
 */
public record StandingOnCondition(HolderSet<Block> blocks) implements ClassCondition {

    public static final MapCodec<StandingOnCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(StandingOnCondition::blocks)
    ).apply(i, StandingOnCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.STANDING_ON;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.onGround() && player.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement()).is(blocks);
    }
}

package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.core.PipicanRegistries;
import com.pipican.stat.StatDefinition;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;

/**
 * Server -> client: the player's total in each stat (class base + points put in + bonuses from passives and tree
 * nodes), as computed by {@link com.pipican.player.ClassEffectApplier}. Only the server can work bonuses out, so
 * the level-up screen reads them from here. Sent whenever they change.
 */
public record StatTotalsPayload(Map<ResourceKey<StatDefinition>, Double> totals) implements CustomPacketPayload {
    public static final Type<StatTotalsPayload> TYPE = new Type<>(Pipican.id("stat_totals"));
    public static final StreamCodec<ByteBuf, StatTotalsPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, ResourceKey<StatDefinition>, Double, Map<ResourceKey<StatDefinition>, Double>>map(
                            HashMap::new, ResourceKey.streamCodec(PipicanRegistries.STAT), ByteBufCodecs.DOUBLE)
                    .map(StatTotalsPayload::new, StatTotalsPayload::totals);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

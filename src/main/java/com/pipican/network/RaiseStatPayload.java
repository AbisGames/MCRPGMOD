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
 * Client -> server: the level-up screen's "Accept". Puts the given number of stat points into each stat, all or
 * nothing (see {@link com.pipican.player.ClassService#raiseStats}).
 */
public record RaiseStatPayload(Map<ResourceKey<StatDefinition>, Integer> points) implements CustomPacketPayload {
    public static final Type<RaiseStatPayload> TYPE = new Type<>(Pipican.id("raise_stat"));
    /** More stats than any pack ships; keeps a malicious client from sending a huge map. */
    private static final int MAX_ENTRIES = 256;
    public static final StreamCodec<ByteBuf, RaiseStatPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, ResourceKey<StatDefinition>, Integer, Map<ResourceKey<StatDefinition>, Integer>>map(
                            HashMap::new, ResourceKey.streamCodec(PipicanRegistries.STAT), ByteBufCodecs.VAR_INT, MAX_ENTRIES)
                    .map(RaiseStatPayload::new, RaiseStatPayload::points);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.core.PipicanRegistries;
import com.pipican.stat.StatDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;

/** Client -> server: buy one point in a stat. Ready for the upcoming character screen. */
public record RaiseStatPayload(ResourceKey<StatDefinition> stat) implements CustomPacketPayload {
    public static final Type<RaiseStatPayload> TYPE = new Type<>(Pipican.id("raise_stat"));
    public static final StreamCodec<ByteBuf, RaiseStatPayload> STREAM_CODEC =
            ResourceKey.streamCodec(PipicanRegistries.STAT).map(RaiseStatPayload::new, RaiseStatPayload::stat);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

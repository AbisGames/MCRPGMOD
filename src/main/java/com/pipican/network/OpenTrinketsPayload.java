package com.pipican.network;

import com.pipican.Pipican;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the trinket key was pressed; the server opens the trinket menu. */
public record OpenTrinketsPayload() implements CustomPacketPayload {
    public static final OpenTrinketsPayload INSTANCE = new OpenTrinketsPayload();
    public static final Type<OpenTrinketsPayload> TYPE = new Type<>(Pipican.id("open_trinkets"));
    public static final StreamCodec<ByteBuf, OpenTrinketsPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

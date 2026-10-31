package com.pipican.network;

import com.pipican.Pipican;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server response authorizing the client to open the blacksmith screen. */
public record OpenBlacksmithPayload() implements CustomPacketPayload {
    public static final OpenBlacksmithPayload INSTANCE = new OpenBlacksmithPayload();
    public static final Type<OpenBlacksmithPayload> TYPE = new Type<>(Pipican.id("open_blacksmith"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, OpenBlacksmithPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

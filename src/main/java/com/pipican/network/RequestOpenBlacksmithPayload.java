package com.pipican.network;

import com.pipican.Pipican;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client request to open the server-authorized blacksmith transaction screen. */
public record RequestOpenBlacksmithPayload() implements CustomPacketPayload {
    public static final RequestOpenBlacksmithPayload INSTANCE = new RequestOpenBlacksmithPayload();
    public static final Type<RequestOpenBlacksmithPayload> TYPE = new Type<>(Pipican.id("request_open_blacksmith"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, RequestOpenBlacksmithPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

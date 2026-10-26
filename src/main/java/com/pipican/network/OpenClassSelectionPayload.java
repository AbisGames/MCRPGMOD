package com.pipican.network;

import com.pipican.Pipican;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: "open the class-selection screen". The class list comes from the synced registry. */
public record OpenClassSelectionPayload() implements CustomPacketPayload {
    public static final OpenClassSelectionPayload INSTANCE = new OpenClassSelectionPayload();
    public static final Type<OpenClassSelectionPayload> TYPE = new Type<>(Pipican.id("open_class_selection"));
    public static final StreamCodec<ByteBuf, OpenClassSelectionPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

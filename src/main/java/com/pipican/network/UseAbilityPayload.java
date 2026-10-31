package com.pipican.network;

import com.pipican.Pipican;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: an ability key (slot 0-3) was pressed. The server works out which ability that is. */
public record UseAbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<UseAbilityPayload> TYPE = new Type<>(Pipican.id("use_ability"));
    public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(UseAbilityPayload::new, UseAbilityPayload::slot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.core.PipicanRegistries;
import com.pipican.rpgclass.RpgClass;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;

/** Client -> server: the player picked a class on the selection screen. */
public record ChooseClassPayload(ResourceKey<RpgClass> rpgClass) implements CustomPacketPayload {
    public static final Type<ChooseClassPayload> TYPE = new Type<>(Pipican.id("choose_class"));
    public static final StreamCodec<ByteBuf, ChooseClassPayload> STREAM_CODEC =
            ResourceKey.streamCodec(PipicanRegistries.CLASS).map(ChooseClassPayload::new, ChooseClassPayload::rpgClass);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.pipican.network;

import com.pipican.Pipican;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the passive-tree screen (from {@code /pipican tree}). All data is already synced. */
public record OpenSkillTreePayload() implements CustomPacketPayload {
    public static final OpenSkillTreePayload INSTANCE = new OpenSkillTreePayload();
    public static final Type<OpenSkillTreePayload> TYPE = new Type<>(Pipican.id("open_skill_tree"));
    public static final StreamCodec<ByteBuf, OpenSkillTreePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

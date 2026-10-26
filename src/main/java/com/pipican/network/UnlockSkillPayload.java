package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.skill.TreeKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: buy one rank of a skill node. Ready for the upcoming skill-tree screen. */
public record UnlockSkillPayload(TreeKind tree, String nodeId) implements CustomPacketPayload {
    public static final Type<UnlockSkillPayload> TYPE = new Type<>(Pipican.id("unlock_skill"));
    public static final StreamCodec<ByteBuf, UnlockSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.map(b -> b ? TreeKind.SPECIAL : TreeKind.GENERAL, k -> k == TreeKind.SPECIAL), UnlockSkillPayload::tree,
            ByteBufCodecs.stringUtf8(128), UnlockSkillPayload::nodeId,
            UnlockSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.skill.TreeKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

/** Client -> server: allocate ({@code refund == false}) or refund a passive-tree node. Sent by the tree screen. */
public record NodePayload(TreeKind tree, String nodeId, boolean refund) implements CustomPacketPayload {
    public static final Type<NodePayload> TYPE = new Type<>(Pipican.id("tree_node"));
    public static final StreamCodec<ByteBuf, NodePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(ByIdMap.continuous(TreeKind::ordinal, TreeKind.values(), ByIdMap.OutOfBoundsStrategy.ZERO), TreeKind::ordinal), NodePayload::tree,
            ByteBufCodecs.stringUtf8(128), NodePayload::nodeId,
            ByteBufCodecs.BOOL, NodePayload::refund,
            NodePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

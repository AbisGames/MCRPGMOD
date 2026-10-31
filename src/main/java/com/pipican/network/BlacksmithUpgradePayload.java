package com.pipican.network;

import com.pipican.Pipican;
import com.pipican.blacksmith.BlacksmithUpgrades.Action;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Client request containing the complete expected stack snapshot; the server revalidates every field. */
public record BlacksmithUpgradePayload(UUID nonce, int slot, long expectedRevision, ItemStack expected,
                                       Action action, String profile) implements CustomPacketPayload {
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = ByteBufCodecs.stringUtf8(36).map(UUID::fromString, UUID::toString);
    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.VAR_INT.map(
            value -> value == 0 ? Action.UPGRADE : Action.EVOLVE,
            action -> action == Action.UPGRADE ? 0 : 1);

    public static final Type<BlacksmithUpgradePayload> TYPE = new Type<>(Pipican.id("blacksmith_upgrade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlacksmithUpgradePayload> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, BlacksmithUpgradePayload::nonce,
            ByteBufCodecs.VAR_INT, BlacksmithUpgradePayload::slot,
            ByteBufCodecs.VAR_LONG, BlacksmithUpgradePayload::expectedRevision,
            ItemStack.STREAM_CODEC, BlacksmithUpgradePayload::expected,
            ACTION_CODEC, BlacksmithUpgradePayload::action,
            ByteBufCodecs.stringUtf8(64), BlacksmithUpgradePayload::profile,
            BlacksmithUpgradePayload::new);

    public BlacksmithUpgradePayload {
        expected = expected.copy();
        if (slot < 0 || slot >= 36 || expected.isEmpty() || profile.length() > 64) {
            throw new IllegalArgumentException("Invalid blacksmith request");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

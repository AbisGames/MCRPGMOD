package com.pipican.network;

import com.pipican.ability.AbilityService;
import com.pipican.player.ClassService;
import com.pipican.blacksmith.BlacksmithUpgrades;
import com.pipican.trinket.TrinketMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every packet. Server-bound handlers live here; client-bound handlers are registered
 * from the client side in {@link com.pipican.client.PipicanClient} so no client classes load on a
 * dedicated server.
 *
 * <p>Note: the player's class data itself is synced by the attachment system, not by these packets.
 */
public final class PipicanNetwork {
    private static final String VERSION = "1";

    private PipicanNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(VERSION);

        r.playToClient(OpenClassSelectionPayload.TYPE, OpenClassSelectionPayload.STREAM_CODEC);
        r.playToClient(OpenSkillTreePayload.TYPE, OpenSkillTreePayload.STREAM_CODEC);
        r.playToClient(OpenBlacksmithPayload.TYPE, OpenBlacksmithPayload.STREAM_CODEC);
        r.playToClient(StatTotalsPayload.TYPE, StatTotalsPayload.STREAM_CODEC);

        r.playToServer(ChooseClassPayload.TYPE, ChooseClassPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ClassService.Result result = ClassService.chooseClass(player, payload.rpgClass(), true, false);
            player.sendSystemMessage(result.message());
        });
        r.playToServer(OpenTrinketsPayload.TYPE, OpenTrinketsPayload.STREAM_CODEC,
                (payload, ctx) -> TrinketMenu.open((ServerPlayer) ctx.player()));
        r.playToServer(RequestOpenBlacksmithPayload.TYPE, RequestOpenBlacksmithPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (!BlacksmithUpgrades.profiles().isEmpty()) {
                PacketDistributor.sendToPlayer(player, OpenBlacksmithPayload.INSTANCE);
            }
        });
        r.playToServer(BlacksmithUpgradePayload.TYPE, BlacksmithUpgradePayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            var result = BlacksmithUpgrades.execute(player, new BlacksmithUpgrades.Request(payload.nonce(), payload.slot(),
                    payload.expectedRevision(), payload.expected(), payload.action(), payload.profile()));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "blacksmith.pipican.result." + result.name().toLowerCase(java.util.Locale.ROOT)));
        });
        r.playToServer(RaiseStatPayload.TYPE, RaiseStatPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ClassService.Result result = ClassService.raiseStats(player, payload.points());
            player.sendOverlayMessage(result.message());
        });
        r.playToServer(UseAbilityPayload.TYPE, UseAbilityPayload.STREAM_CODEC,
                (payload, ctx) -> AbilityService.useSlot((ServerPlayer) ctx.player(), payload.slot()));
        r.playToServer(NodePayload.TYPE, NodePayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ClassService.Result result = payload.refund()
                    ? ClassService.refundNode(player, payload.tree(), payload.nodeId())
                    : ClassService.allocateNode(player, payload.tree(), payload.nodeId());
            // Success is visible in the tree screen itself; only explain failures.
            if (!result.success()) {
                player.sendOverlayMessage(result.message());
            }
        });
    }
}

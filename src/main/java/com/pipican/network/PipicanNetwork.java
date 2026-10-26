package com.pipican.network;

import com.pipican.player.ClassService;
import com.pipican.trinket.TrinketMenu;
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

        r.playToServer(ChooseClassPayload.TYPE, ChooseClassPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ClassService.Result result = ClassService.chooseClass(player, payload.rpgClass(), true, false);
            player.sendSystemMessage(result.message());
        });
        r.playToServer(OpenTrinketsPayload.TYPE, OpenTrinketsPayload.STREAM_CODEC,
                (payload, ctx) -> TrinketMenu.open((ServerPlayer) ctx.player()));
        r.playToServer(RaiseStatPayload.TYPE, RaiseStatPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            player.sendSystemMessage(ClassService.raiseStat(player, payload.stat()).message());
        });
        r.playToServer(UnlockSkillPayload.TYPE, UnlockSkillPayload.STREAM_CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            player.sendSystemMessage(ClassService.unlockNode(player, payload.tree(), payload.nodeId()).message());
        });
    }
}

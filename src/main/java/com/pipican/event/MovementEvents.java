package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import com.pipican.player.PipicanAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of movement abilities: tells the client which ones the player has (it moves the player itself), and
 * keeps gliding and climbing from counting as falling damage or forbidden flight.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class MovementEvents {

    private static final List<Kind> CLIENT_KINDS = List.of(Kind.NO_COBWEB, Kind.NO_POWDER_SNOW, Kind.DOUBLE_JUMP, Kind.WALL_RUN, Kind.WALL_CLIMB, Kind.GLIDE, Kind.HIDE_NAMEPLATE, Kind.SINK);

    private MovementEvents() {}

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        if (player.tickCount % 10 == 0) {
            List<String> flags = new ArrayList<>();
            for (Kind kind : CLIENT_KINDS) {
                if (state.chance(kind) > 0) {
                    flags.add(kind.name().toLowerCase(Locale.ROOT));
                }
            }
            if (!flags.equals(player.getData(PipicanAttachments.MOVE_FLAGS).flags())) {
                player.setData(PipicanAttachments.MOVE_FLAGS, new PipicanAttachments.MoveFlags(List.copyOf(flags)));
                player.syncData(PipicanAttachments.MOVE_FLAGS);
            }
        }
        if (player.onGround()) {
            return;
        }
        double dy = player.getY() - player.yo;
        boolean glide = state.chance(Kind.GLIDE) > 0 && dy > -0.6 && dy < 0;
        boolean climb = (state.chance(Kind.WALL_CLIMB) > 0 || state.chance(Kind.WALL_RUN) > 0) && player.horizontalCollision;
        if (glide || climb) {
            player.resetFallDistance();
            player.connection.aboveGroundTickCount = 0;
        }
    }
}

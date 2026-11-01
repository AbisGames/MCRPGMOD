package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * {@code pipican:locate} - tells the user which way and how far: their bed / spawn point, or the
 * nearest structure from a structure tag.
 *
 * <pre>{@code { "type": "pipican:locate", "structures": "#minecraft:village" } }</pre>
 * {@code any_of}: several structure tags - it points to whichever is nearest ("the nearest structure").
 * Without either it points to the user's respawn point (or the world spawn).
 */
public record LocateAction(Optional<TagKey<Structure>> structures, java.util.List<TagKey<Structure>> anyOf) implements AbilityAction {

    public static final MapCodec<LocateAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TagKey.hashedCodec(Registries.STRUCTURE).optionalFieldOf("structures").forGetter(LocateAction::structures),
            TagKey.hashedCodec(Registries.STRUCTURE).listOf().optionalFieldOf("any_of", java.util.List.of()).forGetter(LocateAction::anyOf)
    ).apply(i, LocateAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.LOCATE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        BlockPos target;
        Component what;
        if (!anyOf.isEmpty()) {
            target = null;
            for (TagKey<Structure> tag : anyOf) {
                BlockPos found = ctx.level().findNearestMapStructure(tag, player.blockPosition(), 48, false);
                if (found != null && (target == null || found.distSqr(player.blockPosition()) < target.distSqr(player.blockPosition()))) {
                    target = found;
                }
            }
            what = Component.translatable("pipican.locate.structure");
        } else if (structures.isPresent()) {
            target = ctx.level().findNearestMapStructure(structures.get(), player.blockPosition(), 64, false);
            what = Component.translatable("pipican.locate.structure");
        } else {
            var config = player.getRespawnConfig();
            var data = config != null ? config.respawnData() : ctx.level().getRespawnData();
            target = data.dimension().equals(player.level().dimension()) ? data.pos() : null;
            what = Component.translatable("pipican.locate.home");
        }
        if (target == null) {
            ctx.cancel(Component.translatable("pipican.locate.nothing"));
            return;
        }
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        // Minecraft yaw: 0 = south (+z), 90 = west (-x).
        float bearing = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        String[] names = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};
        String dir = names[Math.floorMod(Math.round(bearing / 45f), 8)];
        player.sendSystemMessage(Component.translatable("pipican.locate.result", what, distance,
                Component.translatable("pipican.direction." + dir)));
    }
}

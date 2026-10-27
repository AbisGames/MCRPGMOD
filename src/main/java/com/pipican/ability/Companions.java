package com.pipican.ability;

import com.pipican.entity.Eidolon;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

/** The {@code companions} target: a player's summons, tamed pets and eidolon within a radius. */
public final class Companions {

    private Companions() {}

    public static List<LivingEntity> of(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive()
                && (e instanceof OwnableEntity own && own.getOwner() == player
                || Summons.isSummonOf(e, player) || MobControl.isAllyOf(e, player)
                || e instanceof Eidolon eidolon && eidolon.isOwnedBy(player)));
    }
}

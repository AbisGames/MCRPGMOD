package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Summons;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code pipican:charge} - the user (or their mount) charges the way they look; every mob they run into during the next
 * {@code seconds} takes {@code damage} as a melee hit and is knocked back by {@code push} - the hit lands on impact.
 *
 * <pre>{@code { "type": "pipican:charge", "strength": 1.8, "up": 0.1, "damage": 8, "push": 1.5, "seconds": 1 } }</pre>
 */
public record ChargeAction(double strength, double up, double damage, double push, double seconds) implements AbilityAction {

    public static final MapCodec<ChargeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 5).optionalFieldOf("strength", 1.6).forGetter(ChargeAction::strength),
            Codec.doubleRange(-2, 3).optionalFieldOf("up", 0.1).forGetter(ChargeAction::up),
            Codec.doubleRange(0, 1000).optionalFieldOf("damage", 6.0).forGetter(ChargeAction::damage),
            Codec.doubleRange(0, 10).optionalFieldOf("push", 1.0).forGetter(ChargeAction::push),
            Codec.doubleRange(0.1, 10).optionalFieldOf("seconds", 1.0).forGetter(ChargeAction::seconds)
    ).apply(i, ChargeAction::new));

    private record Charging(ServerPlayer player, long until, double damage, double push, Vec3 direction, Set<UUID> hit) {}

    private static final Map<UUID, Charging> CHARGING = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CHARGE;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Entity mover = player.getVehicle() != null ? player.getVehicle() : player;
        mover.setDeltaMovement(mover.getDeltaMovement().add(look.x * strength, up, look.z * strength));
        mover.hurtMarked = true;
        CHARGING.put(player.getUUID(), new Charging(player, ctx.level().getGameTime() + Math.round((seconds + ctx.mod().seconds()) * 20),
                damage + ctx.mod().damage(), push, look, new HashSet<>()));
    }

    public static boolean charging(Player player) {
        return CHARGING.containsKey(player.getUUID());
    }

    @EventBusSubscriber(modid = Pipican.MOD_ID)
    public static final class Ticker {
        private Ticker() {}

        @SubscribeEvent
        static void onTick(ServerTickEvent.Post event) {
            for (var it = CHARGING.values().iterator(); it.hasNext(); ) {
                Charging c = it.next();
                ServerPlayer player = c.player();
                if (!player.isAlive() || player.level().getGameTime() > c.until()) {
                    it.remove();
                    continue;
                }
                Entity mover = player.getVehicle() != null ? player.getVehicle() : player;
                for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, mover.getBoundingBox().inflate(0.6),
                        e -> e != player && e != mover && e.isAlive() && !(e instanceof Player) && !Summons.isSummonOf(e, player))) {
                    if (c.hit().add(mob.getUUID())) {
                        mob.invulnerableTime = 0;
                        mob.hurtServer(player.level(), player.damageSources().playerAttack(player), (float) c.damage());
                        mob.knockback(c.push(), -c.direction().x, -c.direction().z);
                        mob.hurtMarked = true;
                    }
                }
            }
        }
    }
}

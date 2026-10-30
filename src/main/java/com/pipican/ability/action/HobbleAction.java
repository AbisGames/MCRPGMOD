package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * {@code pipican:hobble} - the reaction's mob can't jump ({@code "root": false}) or can't move at all ({@code "root": true})
 * for {@code seconds}: "Lethargy also stops jumping", "Slowness becomes immobilise for 1s". With {@code if_effect}, only a
 * mob that has that effect is hobbled. Players aren't affected (their client moves them).
 *
 * <pre>{@code { "type": "pipican:hobble", "seconds": 8, "if_effect": "minecraft:slowness" } }</pre>
 */
public record HobbleAction(double seconds, boolean root, Optional<Holder<MobEffect>> ifEffect) implements AbilityAction {

    public static final MapCodec<HobbleAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.05, 600).fieldOf("seconds").forGetter(HobbleAction::seconds),
            Codec.BOOL.optionalFieldOf("root", false).forGetter(HobbleAction::root),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().optionalFieldOf("if_effect").forGetter(HobbleAction::ifEffect)
    ).apply(i, HobbleAction::new));

    /** Entity -> game time it may jump again / move again. */
    private static final Map<UUID, Long> NO_JUMP = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ROOTED = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.HOBBLE;
    }

    @Override
    public void run(AbilityContext ctx) {
        long until = ctx.level().getGameTime() + Math.round((seconds + ctx.mod().seconds()) * 20);
        ctx.subject().ifPresent(e -> {
            if (e instanceof Player || ifEffect.isPresent() && !(e instanceof LivingEntity l && l.hasEffect(ifEffect.get()))) {
                return;
            }
            (root ? ROOTED : NO_JUMP).merge(e.getUUID(), until, Math::max);
        });
    }

    public static boolean noJump(Entity entity) {
        return active(NO_JUMP, entity) || active(ROOTED, entity);
    }

    public static boolean rooted(Entity entity) {
        return active(ROOTED, entity);
    }

    private static boolean active(Map<UUID, Long> map, Entity entity) {
        Long until = map.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() >= until || !entity.isAlive()) {
            map.remove(entity.getUUID());
            return false;
        }
        return true;
    }
}

package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Summons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;

/**
 * {@code pipican:summon} - spawns temporary helpers next to the user. Tameable animals (wolves, cats)
 * are tamed to the user and iron golems count as player-made, so both fight for them. Summons vanish
 * after {@code seconds} and drop nothing.
 *
 * <pre>{@code { "type": "pipican:summon", "entity": "minecraft:wolf", "count": 2, "seconds": 30 } }</pre>
 */
public record SummonAction(EntityType<?> entity, int count, double seconds) implements AbilityAction {

    public static final MapCodec<SummonAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(SummonAction::entity),
            Codec.intRange(1, 16).optionalFieldOf("count", 1).forGetter(SummonAction::count),
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 30.0).forGetter(SummonAction::seconds)
    ).apply(i, SummonAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.SUMMON;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        for (int n = 0; n < count; n++) {
            var spawned = entity.create(ctx.level(), EntitySpawnReason.MOB_SUMMONED);
            if (!(spawned instanceof Mob mob)) {
                continue;
            }
            double angle = Math.PI * 2 * n / count;
            mob.snapTo(player.getX() + Math.cos(angle) * 1.5, player.getY(), player.getZ() + Math.sin(angle) * 1.5, player.getYRot(), 0);
            if (mob instanceof TamableAnimal pet) {
                pet.tame(player);
            }
            if (mob instanceof IronGolem golem) {
                golem.setPlayerCreated(true);
            }
            mob.setPersistenceRequired();
            ctx.level().addFreshEntity(mob);
            Summons.track(mob, player, (long) (seconds * 20));
        }
    }
}

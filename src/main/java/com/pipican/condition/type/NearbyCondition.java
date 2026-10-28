package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * {@code pipican:nearby} - how many other players or hostile mobs are within {@code radius} blocks.
 * Both bounds inclusive; the player never counts themselves.
 *
 * <pre>{@code { "type": "pipican:nearby", "entities": "players", "radius": 16, "max": 0 } }   alone</pre>
 * Defaults: {@code radius} 8, {@code min} 1, no {@code max}. {@code pets} counts the player's own tamed animals;
 * {@code entity} narrows any kind to one entity type (e.g. {@code "minecraft:cat"}).
 */
public record NearbyCondition(Kind entities, double radius, int min, Optional<Integer> max,
                              Optional<net.minecraft.world.entity.EntityType<?>> entity) implements ClassCondition {

    public enum Kind implements StringRepresentable {
        PLAYERS("players"), HOSTILE_MOBS("hostile_mobs"), PETS("pets");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<NearbyCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("entities").forGetter(NearbyCondition::entities),
            Codec.doubleRange(0.5, 64).optionalFieldOf("radius", 8.0).forGetter(NearbyCondition::radius),
            Codec.intRange(0, 1000).optionalFieldOf("min", 1).forGetter(NearbyCondition::min),
            Codec.intRange(0, 1000).optionalFieldOf("max").forGetter(NearbyCondition::max),
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(NearbyCondition::entity)
    ).apply(i, NearbyCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.NEARBY;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var box = player.getBoundingBox().inflate(radius);
        double r2 = radius * radius;
        int count = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()
                && e.distanceToSqr(player) <= r2
                && (entity.isEmpty() || e.getType() == entity.get())
                && switch (entities) {
                    case PLAYERS -> e instanceof Player p && !p.isSpectator();
                    case HOSTILE_MOBS -> e instanceof Enemy;
                    case PETS -> e instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwnerReference() != null
                            && o.getOwnerReference().getUUID().equals(player.getUUID());
                }).size();
        return count >= min && (max.isEmpty() || count <= max.get());
    }
}

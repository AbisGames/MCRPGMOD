package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code pipican:attribute_boost} - changes the targets' attributes for {@code seconds} (Overdrive: "eidolon +50%
 * speed and damage for 10s"). Using it again while it lasts restarts the timer.
 *
 * <pre>{@code { "type": "pipican:attribute_boost", "target": "companions", "radius": 32, "seconds": 10, "attributes": [
 *     { "attribute": "minecraft:movement_speed", "amount": 0.5, "operation": "add_multiplied_base" } ] } }</pre>
 */
public record AttributeBoostAction(Targets targets, double seconds, List<Entry> attributes) implements AbilityAction {

    public record Entry(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(Entry::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Entry::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_MULTIPLIED_BASE).forGetter(Entry::operation)
        ).apply(i, Entry::new));
    }

    public static final MapCodec<AttributeBoostAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.COMPANIONS).forGetter(AttributeBoostAction::targets),
            Codec.doubleRange(0.05, 3600).optionalFieldOf("seconds", 10.0).forGetter(AttributeBoostAction::seconds),
            Entry.CODEC.listOf().fieldOf("attributes").forGetter(AttributeBoostAction::attributes)
    ).apply(i, AttributeBoostAction::new));

    private static final Identifier ID = Pipican.id("attribute_boost");

    private record Running(LivingEntity entity, List<Entry> attributes) {}

    /** Boosted entity -> game time the boost ends. */
    private static final Map<Running, Long> RUNNING = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.ATTRIBUTE_BOOST;
    }

    @Override
    public void run(AbilityContext ctx) {
        long until = ctx.level().getGameTime() + Math.round((seconds + ctx.mod().seconds()) * 20);
        double k = 1 + ctx.mod().strength();
        for (LivingEntity target : targets.select(ctx)) {
            for (Entry e : attributes) {
                var instance = target.getAttribute(e.attribute());
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(new AttributeModifier(ID, e.amount() * k, e.operation()));
                }
            }
            RUNNING.put(new Running(target, attributes), until);
        }
    }

    @EventBusSubscriber(modid = Pipican.MOD_ID)
    public static final class Ticker {
        private Ticker() {}

        @SubscribeEvent
        static void onTick(ServerTickEvent.Post event) {
            for (var it = RUNNING.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                LivingEntity entity = entry.getKey().entity();
                if (!entity.isAlive() || entity.level().getGameTime() >= entry.getValue()) {
                    for (Entry e : entry.getKey().attributes()) {
                        var instance = entity.getAttribute(e.attribute());
                        if (instance != null) {
                            instance.removeModifier(ID);
                        }
                    }
                    it.remove();
                }
            }
        }
    }
}

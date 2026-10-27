package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:on} - runs ability actions when something happens: "your hits have a 10% chance to set
 * mobs on fire", "kills give Speed I for 3s", "mobs that hit you are poisoned".
 *
 * <pre>{@code
 * { "type": "pipican:on", "event": "melee_hit", "chance": 0.1, "cooldown_ticks": 40,
 *   "weapons": "#pipican:weapons/swords", "victims": "#minecraft:undead",
 *   "actions": [ { "type": "pipican:effect", "target": "victim", "effects": [ { "effect": "minecraft:wither", "seconds": 2 } ] } ] }
 * }</pre>
 * Events: {@code hit} (you damage something, melee or ranged), {@code melee_hit}, {@code ranged_hit},
 * {@code kill}, {@code hurt} (something damages you), {@code block} (you block with a shield).
 * In the actions, {@code "target": "victim"} is the other side of the event (the mob hit or killed, or the
 * attacker), and area targets are centred on it. Reactions never trigger other reactions.
 */
public record ReactionEffect(Event event, double chance, int cooldownTicks, Optional<HolderSet<Item>> weapons,
                             Optional<HolderSet<EntityType<?>>> victims, List<AbilityAction> actions) implements ClassEffect {

    public enum Event implements StringRepresentable {
        HIT("hit"), MELEE_HIT("melee_hit"), RANGED_HIT("ranged_hit"), KILL("kill"), HURT("hurt"), BLOCK("block");

        public static final Codec<Event> CODEC = StringRepresentable.fromEnum(Event::values);
        private final String name;

        Event(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<ReactionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Event.CODEC.fieldOf("event").forGetter(ReactionEffect::event),
            Codec.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(ReactionEffect::chance),
            Codec.intRange(0, 72_000).optionalFieldOf("cooldown_ticks", 0).forGetter(ReactionEffect::cooldownTicks),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(ReactionEffect::weapons),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("victims").forGetter(ReactionEffect::victims),
            Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().fieldOf("actions").forGetter(ReactionEffect::actions)
    ).apply(i, ReactionEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.ON;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addReaction(this);
    }
}

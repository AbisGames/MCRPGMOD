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
 * attacker), and area targets are centred on it. {@code victim} (a {@link com.pipican.effect.VictimState}) limits
 * the reaction to a mob in that state ("hitting a mob from behind"). {@code cost} is resource the reaction needs and
 * spends ("heals them 6 HP, spends 20 Miracle"); without enough it doesn't happen. Reactions never trigger other reactions.
 */
public record ReactionEffect(Event event, double chance, int cooldownTicks, Optional<HolderSet<Item>> weapons,
                             Optional<HolderSet<EntityType<?>>> victims, List<AbilityAction> actions, Optional<String> name, boolean oncePerRest, int usesPerRest,
                             Optional<com.pipican.effect.VictimState> victim, double cost) implements ClassEffect {

    public enum Event implements StringRepresentable {
        HIT("hit"), CRIT("crit"), MELEE_HIT("melee_hit"), RANGED_HIT("ranged_hit"), KILL("kill"), HURT("hurt"), BLOCK("block"),
        /** An ability spent some of the resource bar. */
        SPEND("spend"),
        /** Woke up after sleeping. */
        WAKE("wake"),
        /** A hit is about to drop you below 2 hearts (4 health); {@code pipican:shrug} cancels it. */
        LOW_HEALTH("low_health"),
        /** The (conditional) effect holding this reaction starts to count ("when you go Berserk"). */
        START("start"),
        /** The (conditional) effect holding this reaction stops counting ("when Berserk ends"). */
        END("end"),
        /** You are about to die; {@code pipican:cheat_death} saves you. */
        DEATH("death"),
        /** Every second while you're in combat; give it a {@code cooldown_ticks} ("every 30s in combat"). */
        IN_COMBAT("in_combat"),
        /** Every second (usually inside a {@code pipican:conditional}: "while you sneak, nearby mobs glow"). */
        SECOND("second"),
        /** You teleported (ender pearl, chorus fruit, blink). */
        TELEPORT("teleport"),
        /** You played a goat horn or a note block. */
        INSTRUMENT("instrument"),
        /** You were struck by lightning. */
        LIGHTNING("lightning"),
        /** A mob or player is about to hit you (before damage); {@code pipican:shrug} makes it miss ("hits a decoy instead"). */
        ATTACKED("attacked"),
        /** You finished eating; {@code weapons} limits it to some foods. */
        EAT("eat"),
        /** You used (right-clicked) an item; {@code weapons} limits it to some items ("after throwing a wind charge"). */
        USE_ITEM("use_item"),
        /** You finished drinking a potion. */
        DRINK("drink"),
        /** Like {@code attacked}, but only a melee hit (not arrows or spells); {@code pipican:shrug} cancels it. */
        ATTACKED_MELEE("attacked_melee"),
        /** You right-clicked a mob or player (the victim); {@code weapons} limits it to what you hold (air = empty hand). */
        INTERACT("interact"),
        /** An explosion you caused hurt a mob (the victim). */
        EXPLOSION_HIT("explosion_hit"),
        /** One of your executes killed a mob (the victim; area actions centre on it). */
        EXECUTE("execute"),
        /** One of your summons or tamed animals hurt a mob (the victim). */
        PET_HIT("pet_hit"),
        /** You used bone meal on a plant. */
        BONE_MEAL("bone_meal"),
        /** One of your tamed animals died (the victim). */
        PET_DEATH("pet_death"),
        /** A mob hit another player near you (the victim is the mob). */
        MOB_HITS_ALLY("mob_hits_ally"),
        /** Another player hurt a mob near you (the victim is the mob). */
        ALLY_HITS_MOB("ally_hits_mob"),
        /** Another player within 16 blocks took damage (the victim is whoever hurt them, if anyone). */
        ALLY_HURT("ally_hurt"),
        /** Your magic damage hurt a mob. */
        MAGIC_HIT("magic_hit"),
        /** Your quarry (challenged foe...) attacked you and missed: you dodged or blocked it (the victim is the quarry). */
        QUARRY_MISS("quarry_miss"),
        /** Your quarry hurt you (the victim is the quarry). */
        QUARRY_HURT("quarry_hurt"),
        /** {@code spell_ward} cancelled a harmful effect or magic attack aimed at you. */
        WARD("ward"),
        /** Your resource bar just became full. */
        RESOURCE_FULL("resource_full");

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
            Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().fieldOf("actions").forGetter(ReactionEffect::actions),
            Codec.STRING.optionalFieldOf("name").forGetter(ReactionEffect::name),
            Codec.BOOL.optionalFieldOf("once_per_rest", false).forGetter(ReactionEffect::oncePerRest),
            Codec.intRange(0, 1000).optionalFieldOf("uses_per_rest", 0).forGetter(ReactionEffect::usesPerRest),
            com.pipican.effect.VictimState.CODEC.optionalFieldOf("victim").forGetter(ReactionEffect::victim),
            Codec.doubleRange(0, 100_000).optionalFieldOf("cost", 0.0).forGetter(ReactionEffect::cost)
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

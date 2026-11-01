package com.pipican.mastery.trigger;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * {@code pipican:did} - one of the {@link SkillAction.Did.Kind} actions: {@code crit}, {@code bleed} (made a mob bleed),
 * {@code dodge}, {@code spend} (an ability spent resource), {@code heal} (you were healed), {@code teleport} (pearls,
 * chorus fruit, blinks), {@code enchant}, {@code brew}, {@code craft}, {@code smelt}, {@code trade}, {@code tame},
 * {@code breed}, {@code mine}, {@code harvest} (a fully grown crop), {@code plant}, {@code bone_meal}, {@code anvil},
 * {@code level_up}, {@code drink} (a potion), {@code pet_kill} (your pet or summon killed a mob), {@code fish},
 * {@code take_fall} (fall damage taken), {@code hurt} (any damage taken; {@code amount} is the damage),
 * {@code heal_other} (your heal reached another player), {@code execute} (an execute killed a mob), {@code charm}
 * (you charmed or turned a mob), {@code explosion_hit} (your explosion hurt a mob), {@code active_second} (every
 * second while one of your abilities is running; pair with an {@code ability_active} filter), {@code poison} (you
 * poisoned a mob), {@code use_item} (you right-clicked an item; {@code items} narrows it), {@code absorb} (your
 * resource bar took damage for you), {@code safe_fall} (landed from 6+ blocks without fall damage; {@code amount} is the
 * height), {@code second} (every second, for situation filters such as "time spent outdoors at night"),
 * {@code aura_ally_kill} (another player killed a mob inside one of your auras), {@code assist} (another player killed a
 * mob you hit; {@code amount} is the seconds since your last hit), {@code kill_streak} ({@code amount} is your kills in the
 * last 10s, this one included), {@code offhand_combo} (your off-hand swing followed a main-hand hit), {@code eidolon_hit}
 * / {@code eidolon_repair} (your eidolon hurt a mob / you repaired it), {@code new_chunk} (you entered a chunk for the
 * first time), {@code varied_kill} / {@code varied_use} (a kill / combat hit; {@code amount} is how many different weapon
 * types / item types you killed with / fought with today), {@code sneak_unseen} (every second while sneaking near a hostile
 * mob that isn't targeting you; {@code amount} is its distance), {@code ignite} (you set a mob on fire), {@code trim}
 * (applied an armor trim), {@code rename} (renamed an item at an anvil; {@code items} narrows it), {@code focus_combo}
 * (hit with a focus school after using a different one in the last 15s), {@code magic_hit} (your magic damaged a mob),
 * {@code loot_chest} (opened a naturally generated loot container for the first time), {@code optimize} (used
 * Optimization), {@code potion_hit} (a potion you threw splashed a mob), {@code perform} (played a goat horn, note block or
 * jukebox), {@code empowered_hit} (a hit used up a "your next hits" empower, e.g. Escalating Violence).
 *
 * <pre>{@code { "type": "pipican:did", "action": "mine", "blocks": "#c:ores" } }</pre>
 * {@code items} / {@code blocks} narrow it to some items (crafted, enchanted, drunk...) or blocks (mined, planted...).
 */
public record DidTrigger(SkillAction.Did.Kind action, Optional<HolderSet<Item>> items, Optional<HolderSet<Block>> blocks) implements SkillTrigger {

    public static final MapCodec<DidTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            SkillAction.Did.Kind.CODEC.fieldOf("action").forGetter(DidTrigger::action),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(DidTrigger::items),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("blocks").forGetter(DidTrigger::blocks)
    ).apply(i, DidTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.DID;
    }

    @Override
    public double uses(SkillAction action) {
        if (!(action instanceof SkillAction.Did did) || did.kind() != this.action) {
            return 0;
        }
        if (items.isPresent() && (did.item().isEmpty() || !did.item().is(items.get()))) {
            return 0;
        }
        if (blocks.isPresent() && (did.block().isEmpty() || !did.block().get().is(blocks.get()))) {
            return 0;
        }
        return 1;
    }
}

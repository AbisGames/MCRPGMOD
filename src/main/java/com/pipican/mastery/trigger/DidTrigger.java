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
 * {@code take_fall} (fall damage taken), {@code hurt} (any damage taken; {@code amount} is the damage).
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

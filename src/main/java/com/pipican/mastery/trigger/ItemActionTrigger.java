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
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:melee_hit}, {@code pipican:ranged_hit}, {@code pipican:kill}, {@code pipican:block}:
 * the action happened with an item from {@code items} (any item if omitted).
 *
 * <pre>{@code { "type": "pipican:melee_hit", "items": "#pipican:weapons/swords" } }</pre>
 */
public record ItemActionTrigger(Kind kind, Optional<HolderSet<Item>> items) implements SkillTrigger {

    public enum Kind { MELEE_HIT, RANGED_HIT, KILL, BLOCK }

    public static MapCodec<ItemActionTrigger> codec(Kind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemActionTrigger::items)
        ).apply(i, items -> new ItemActionTrigger(kind, items)));
    }

    @Override
    public SkillTriggerTypes.Type type() {
        return switch (kind) {
            case MELEE_HIT -> SkillTriggerTypes.MELEE_HIT;
            case RANGED_HIT -> SkillTriggerTypes.RANGED_HIT;
            case KILL -> SkillTriggerTypes.KILL;
            case BLOCK -> SkillTriggerTypes.BLOCK;
        };
    }

    @Override
    public double uses(SkillAction action) {
        boolean kindMatches = switch (kind) {
            case MELEE_HIT -> action instanceof SkillAction.MeleeHit;
            case RANGED_HIT -> action instanceof SkillAction.RangedHit;
            case KILL -> action instanceof SkillAction.Kill;
            case BLOCK -> action instanceof SkillAction.Block;
        };
        return kindMatches && itemMatches(action.item()) ? 1 : 0;
    }

    private boolean itemMatches(ItemStack stack) {
        return items.isEmpty() || (!stack.isEmpty() && stack.is(items.get()));
    }
}

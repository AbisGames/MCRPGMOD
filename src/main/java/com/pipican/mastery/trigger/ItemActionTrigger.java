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
 * the action happened with an item from {@code items} (any item if omitted), enchanted with one of
 * {@code enchantments} if given ("killing mobs with Looting").
 *
 * <pre>{@code { "type": "pipican:melee_hit", "items": "#pipican:weapons/swords" } }</pre>
 */
public record ItemActionTrigger(Kind kind, Optional<HolderSet<Item>> items,
                                Optional<HolderSet<net.minecraft.world.item.enchantment.Enchantment>> enchantments) implements SkillTrigger {

    public enum Kind { MELEE_HIT, RANGED_HIT, KILL, BLOCK }

    public static MapCodec<ItemActionTrigger> codec(Kind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemActionTrigger::items),
                RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(ItemActionTrigger::enchantments)
        ).apply(i, (items, enchantments) -> new ItemActionTrigger(kind, items, enchantments)));
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

    @Override
    public double uses(net.minecraft.server.level.ServerPlayer player, SkillAction action) {
        double uses = uses(action);
        if (uses > 0 || items.isEmpty() || action.item().isEmpty() || !com.pipican.player.ClassState.countsAs(player, action.item(), items.get())) {
            return uses;
        }
        // The item counts as one of these weapons for this class: the same check without the item list.
        return new ItemActionTrigger(kind, Optional.empty(), enchantments).uses(action);
    }

    private boolean itemMatches(ItemStack stack) {
        if (items.isPresent() && (stack.isEmpty() || !stack.is(items.get()))) {
            return false;
        }
        if (enchantments.isPresent()) {
            var enchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(stack);
            return enchantments.get().stream().anyMatch(e -> enchants.getLevel(e) > 0);
        }
        return true;
    }
}

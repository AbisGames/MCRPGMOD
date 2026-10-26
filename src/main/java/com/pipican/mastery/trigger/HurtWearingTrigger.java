package com.pipican.mastery.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:hurt_wearing} - taking damage while wearing at least {@code min_pieces} armor
 * pieces from {@code items}. Good for "Heavy Armor"-style skills.
 *
 * <pre>{@code { "type": "pipican:hurt_wearing", "items": "#pipican:armor/heavy", "min_pieces": 2 } }</pre>
 */
public record HurtWearingTrigger(HolderSet<Item> items, int minPieces) implements SkillTrigger {

    public static final MapCodec<HurtWearingTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(HurtWearingTrigger::items),
            Codec.intRange(1, 4).optionalFieldOf("min_pieces", 1).forGetter(HurtWearingTrigger::minPieces)
    ).apply(i, HurtWearingTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.HURT_WEARING;
    }

    @Override
    public double uses(SkillAction action) {
        if (action instanceof SkillAction.HurtWearing hurt) {
            long pieces = hurt.armor().stream().filter(stack -> stack.is(items)).count();
            return pieces >= minPieces ? 1 : 0;
        }
        return 0;
    }
}

package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:craft_bonus} - crafting one of {@code items} gives a second batch with {@code chance}
 * (1 = always: "crafting tipped arrows gives double").
 *
 * <pre>{@code { "type": "pipican:craft_bonus", "items": "#c:dusts/redstone", "chance": 0.005 } }</pre>
 * {@code scaling} applies to {@code chance}.
 */
public record CraftBonusEffect(HolderSet<Item> items, double chance, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<CraftBonusEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(CraftBonusEffect::items),
            Codec.DOUBLE.optionalFieldOf("chance", 0.0).forGetter(CraftBonusEffect::chance),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(CraftBonusEffect::scaling)
    ).apply(i, CraftBonusEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.CRAFT_BONUS;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addCraftBonus(items, Scaling.evaluate(chance, scaling, ctx));
    }
}

package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Ability;
import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanRegistries;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:cast} - Grimoire Casting: reads the first line of the first page of the book and quill
 * (or written book) in either hand, and if it is the name of one of the {@code spells}, runs that
 * ability's actions. Cancelled (and refunded) without a book or a known spell.
 *
 * <pre>{@code { "type": "pipican:cast", "spells": [ "pipican:pyromancer/firebolt", "pipican:traveler/blink" ] } }</pre>
 */
public record CastAction(List<ResourceKey<Ability>> spells) implements AbilityAction {

    public static final MapCodec<CastAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.ABILITY).listOf().fieldOf("spells").forGetter(CastAction::spells)
    ).apply(i, CastAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CAST;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        String written = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            var writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            var finished = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writable != null && !writable.pages().isEmpty()) {
                written = writable.pages().getFirst().raw();
            } else if (finished != null && !finished.pages().isEmpty()) {
                written = finished.pages().getFirst().raw().getString();
            }
            if (written != null) {
                break;
            }
        }
        if (written == null) {
            ctx.cancel(Component.translatable("pipican.ability.needs_book"));
            return;
        }
        String spellName = written.strip().split("\\R", 2)[0].strip().toLowerCase(Locale.ROOT);
        var registry = ClassLookup.abilities(ctx.level().registryAccess());
        for (ResourceKey<Ability> key : spells) {
            Ability spell = registry.getValue(key);
            if (spell != null && spell.name().getString().toLowerCase(Locale.ROOT).equals(spellName)) {
                for (AbilityAction action : spell.actions()) {
                    action.run(ctx);
                    if (ctx.isCancelled()) {
                        return;
                    }
                }
                return;
            }
        }
        ctx.cancel(Component.translatable("pipican.ability.unknown_spell", spellName));
    }
}

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
 * {@code pipican:cast} - Grimoire Casting: each page of the book and quill (or written book) in either hand holds a
 * spell - the name of one of the {@code spells} on its first line. Using the ability casts the spell on the page the
 * grimoire is open at (running that ability's actions); using it while sneaking turns to the next page with a spell
 * (free). Cancelled (and refunded) without a book or a known spell.
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

    /** Player -> the grimoire page they have turned to (sneak + use turns the page). Not saved. */
    private static final java.util.Map<java.util.UUID, Integer> PAGE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The first line of every page of the book and quill (or written book) in either hand; empty without one. */
    private static java.util.List<String> pages(net.minecraft.server.level.ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            var writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            var finished = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            java.util.List<String> raw = writable != null ? writable.pages().stream().map(p -> p.raw()).toList()
                    : finished != null ? finished.pages().stream().map(p -> p.raw().getString()).toList() : java.util.List.of();
            if (!raw.isEmpty()) {
                return raw.stream().map(text -> text.strip().split("\\R", 2)[0].strip().toLowerCase(Locale.ROOT)).toList();
            }
        }
        return java.util.List.of();
    }

    private Ability spell(AbilityContext ctx, String name) {
        var registry = ClassLookup.abilities(ctx.level().registryAccess());
        for (ResourceKey<Ability> key : spells) {
            Ability spell = registry.getValue(key);
            if (spell != null && spell.name().getString().toLowerCase(Locale.ROOT).equals(name)) {
                return spell;
            }
        }
        return null;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        java.util.List<String> pages = pages(player);
        if (pages.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.needs_book"));
            return;
        }
        int page = Math.floorMod(PAGE.getOrDefault(player.getUUID(), 0), pages.size());
        if (player.isShiftKeyDown()) {
            // Turn to the next page that holds a spell this grimoire can cast (free: the use is called off).
            for (int step = 1; step <= pages.size(); step++) {
                int next = (page + step) % pages.size();
                Ability found = spell(ctx, pages.get(next));
                if (found != null) {
                    PAGE.put(player.getUUID(), next);
                    ctx.cancel(Component.translatable("pipican.ability.grimoire_page", next + 1, found.name()));
                    return;
                }
            }
            ctx.cancel(Component.translatable("pipican.ability.unknown_spell", pages.get(page)));
            return;
        }
        Ability spell = spell(ctx, pages.get(page));
        if (spell == null) {
            ctx.cancel(Component.translatable("pipican.ability.unknown_spell", pages.get(page)));
            return;
        }
        for (AbilityAction action : spell.actions()) {
            action.run(ctx);
            if (ctx.isCancelled()) {
                return;
            }
        }
    }
}

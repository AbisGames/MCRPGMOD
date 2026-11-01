package com.pipican.ability;

import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Savant focus schools: the player picks elemental schools ({@code /pipican focus <school>}) up to their
 * {@code focus_slots}; damage of a picked school is {@code focus_bonus} stronger, of the others {@code non_focus_bonus}.
 * Life is healing: the player's heals grow the same way.
 */
public final class Focus {

    public static final List<String> SCHOOLS = List.of("fire", "water", "air", "earth", "storm", "shadow", "light", "life");
    private static final TagKey<DamageType> WITHER = TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, com.pipican.Pipican.id("is_wither"));
    private static final TagKey<DamageType> FALLING_BLOCK = TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, com.pipican.Pipican.id("is_falling_block"));

    private Focus() {}

    public static int slots(ServerPlayer player) {
        return (int) Math.round(ClassEffectApplier.state(player).chance(Kind.FOCUS_SLOTS));
    }

    public static List<String> chosen(ServerPlayer player) {
        return ClassService.data(player).extras().focusSchools;
    }

    public static ClassService.Result choose(ServerPlayer player, String school) {
        String s = school.toLowerCase(Locale.ROOT);
        int slots = slots(player);
        List<String> picked = chosen(player);
        if (slots <= 0) {
            return ClassService.Result.fail(Component.translatable("pipican.focus.none"));
        }
        if (!SCHOOLS.contains(s)) {
            return ClassService.Result.fail(Component.translatable("pipican.focus.unknown", school, String.join(", ", SCHOOLS)));
        }
        if (picked.contains(s)) {
            return ClassService.Result.fail(Component.translatable("pipican.focus.already", s));
        }
        if (picked.size() >= slots) {
            return ClassService.Result.fail(Component.translatable("pipican.focus.full", String.join(", ", picked)));
        }
        picked.add(s);
        ClassService.changed(player);
        return ClassService.Result.ok(Component.translatable("pipican.focus.chosen", s, picked.size(), slots));
    }

    /** Which school a hit belongs to (several can match). */
    private static boolean matches(String school, ServerPlayer attacker, DamageSource source, LivingEntity victim) {
        var direct = source.getDirectEntity();
        return switch (school) {
            case "fire" -> source.is(DamageTypeTags.IS_FIRE) || direct instanceof net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
            case "water" -> direct instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident || attacker.isInWater();
            case "air" -> source.is(DamageTypeTags.IS_FALL) || source.is(net.minecraft.world.damagesource.DamageTypes.MACE_SMASH)
                    || direct instanceof net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
            case "earth" -> source.is(DamageTypeTags.IS_EXPLOSION) || source.is(FALLING_BLOCK);
            case "storm" -> source.is(DamageTypeTags.IS_LIGHTNING);
            case "shadow" -> source.is(WITHER) || attacker.level().getMaxLocalRawBrightness(attacker.blockPosition()) <= 7;
            case "light" -> victim.is(EntityTypeTags.UNDEAD);
            default -> false;
        };
    }

    private static double bonus(ServerPlayer player, ClassState state, String school) {
        return chosen(player).contains(school) ? state.chance(Kind.FOCUS_BONUS) : state.chance(Kind.NON_FOCUS_BONUS);
    }

    /** Damage multiplier for the player's hit. */
    public static double damage(ServerPlayer attacker, DamageSource source, LivingEntity victim) {
        ClassState state = ClassEffectApplier.state(attacker);
        if (state.chance(Kind.FOCUS_SLOTS) <= 0) {
            return 1;
        }
        double mult = 1;
        for (String school : SCHOOLS) {
            if (matches(school, attacker, source, victim)) {
                mult *= 1 + bonus(attacker, state, school);
                used(attacker, school, java.util.Optional.of(victim));
            }
        }
        return mult;
    }

    /** Healing multiplier (the Life school). */
    public static double healing(ServerPlayer healer) {
        ClassState state = ClassEffectApplier.state(healer);
        if (state.chance(Kind.FOCUS_SLOTS) <= 0) {
            return 1;
        }
        used(healer, "life", java.util.Optional.empty());
        return 1 + bonus(healer, state, "life");
    }

    /** One fight: chosen schools used this close together count as used together ("magic of both focus schools in one fight"). */
    private static final long FIGHT_TICKS = 15 * 20;
    private static final java.util.Map<java.util.UUID, java.util.Map<String, Long>> LAST_USED = new java.util.concurrent.ConcurrentHashMap<>();

    /** A chosen school was used: trains {@code focus_combo} when a different chosen school was used in the same fight. */
    private static void used(ServerPlayer player, String school, java.util.Optional<LivingEntity> target) {
        if (!chosen(player).contains(school)) {
            return;
        }
        long now = player.level().getGameTime();
        var last = LAST_USED.computeIfAbsent(player.getUUID(), k -> new java.util.concurrent.ConcurrentHashMap<>());
        boolean other = last.entrySet().stream().anyMatch(e -> !e.getKey().equals(school) && now - e.getValue() <= FIGHT_TICKS);
        last.put(school, now);
        if (other) {
            com.pipican.event.MasteryEvents.did(player, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.FOCUS_COMBO,
                    net.minecraft.world.item.ItemStack.EMPTY, java.util.Optional.empty(), target, 0));
        }
    }
}

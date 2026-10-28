package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * {@code pipican:bonus} - a percentage change to something that isn't an attribute.
 *
 * <pre>{@code { "type": "pipican:bonus", "kind": "experience", "amount": 0.1 } }</pre>
 * {@code amount} 0.1 = +10%, -0.2 = -20%. Amounts of the same kind add up. Kinds:
 * <ul>
 *   <li>{@code experience}: XP you gain</li>
 *   <li>{@code potion_duration} / {@code harmful_duration} / {@code beneficial_duration}: how long effects last on you
 *       (all / harmful / beneficial ones; endless effects are left alone)</li>
 *   <li>{@code applied_duration} / {@code applied_harmful_duration}: how long effects (all / harmful ones) you put on
 *       others last (tipped arrows, thrown potions, abilities...)</li>
 *   <li>{@code hunger}: how fast you get hungry (-0.15 = 15% slower)</li>
 *   <li>{@code trade_price}: villager and wandering trader prices (-0.05 = 5% cheaper)</li>
 *   <li>{@code draw_speed} / {@code reload_speed}: bow and trident draw / crossbow loading speed</li>
 *   <li>{@code resource_gain}: how much the resource bar fills from {@code pipican:resource} actions (hits, kills...)</li>
 *   <li>{@code ability_cost}: the resource cost of abilities (-0.1 = 10% cheaper)</li>
 *   <li>{@code natural_regen}: how fast health comes back from a full food bar</li>
 *   <li>{@code detection_range}: how far away mobs notice you (-0.1 = 10% closer, like sneaking)</li>
 *   <li>{@code skill_training}: how fast skills gain mastery</li>
 *   <li>{@code ability_cooldown}: how long ability cooldowns are (-0.15 = 15% shorter)</li>
 *   <li>{@code durability} / {@code armor_durability} / {@code weapon_durability}: how fast your gear (all / worn armor /
 *       the main-hand item) loses durability (-0.15 = 15% slower, -1 = never)</li>
 *   <li>{@code weapon_repair}: <i>points</i> of durability the main-hand item gets back every 10s (not a %)</li>
 *   <li>{@code armor_floor}: worn armor never drops below this fraction of its durability (0.1 = 10%)</li>
 *   <li>{@code no_sprint}: any amount above 0 = you can not sprint</li>
 *   <li>{@code healing_received}: healing you get from potions, effects, abilities and other players (not natural regen)</li>
 *   <li>{@code temple_radius}: <i>blocks</i> added to the 10-block temple ({@code pipican:temple})</li>
 *   <li>{@code healing_given}: healing your abilities and reactions do</li>
 *   <li>{@code affinity_floor}: any amount above 0 = no gear is worse than neutral for you</li>
 *   <li>{@code offhand_swing}: the share of your melee hits on which the off-hand weapon also strikes (0.5 = every 2nd)</li>
 *   <li>{@code no_shield}: any amount above 0 = you can not block with shields</li>
 *   <li>{@code food_safe}: any amount above 0 = food never gives you harmful effects (rotten flesh, spider eyes...)</li>
 *   <li>{@code no_trample}: any amount above 0 = you never trample farmland</li>
 *   <li>{@code armor_pierce} / {@code ranged_armor_pierce}: the share of the target's armor your hits (all / projectile ones) ignore</li>
 *   <li>{@code projectile_speed}: how fast your arrows fly</li>
 *   <li>{@code extra_arrow}: a <i>chance</i> that a shot fires a second arrow (that can't be picked up)</li>
 *   <li>{@code witch_ward}: any amount above 0 = witches' potions neither hurt you nor give you effects</li>
 *   <li>{@code no_eating_in_combat}: any amount above 0 = you can't start eating while in combat</li>
 *   <li>{@code anvil_safe}: any amount above 0 = anvils never get damaged when you use them</li>
 *   <li>{@code anvil_cost}: anvil level costs (-0.5 = half); {@code anvil_levels}: <i>levels</i> off every anvil use (never below 1)</li>
 *   <li>{@code enchant_levels} / {@code enchant_lapis}: <i>levels</i> / lapis refunded after each enchanting-table enchant (whole numbers)</li>
 *   <li>{@code looting}: <i>Looting levels</i> added to the weapon whenever you kill a mob</li>
 *   <li>{@code drunk_potion_duration}: how long effects from potions you drink last</li>
 *   <li>{@code bone_meal}: a <i>chance</i> that bone meal you use grows the plant one extra step</li>
 *   <li>{@code breeding_speed}: how much sooner animals you breed can breed again (0.1 = 10% shorter wait)</li>
 *   <li>{@code arrow_pierce}: <i>mobs</i> your arrows pass through (whole numbers)</li>
 *   <li>{@code pearl_range}: how far your ender pearls fly</li>
 *   <li>{@code min_melee_damage}: <i>damage</i> your melee hits deal at least</li>
 *   <li>{@code no_sleep}: any amount above 0 = you can't sleep in beds</li>
 *   <li>{@code pearl_cooldown}: the cooldown after throwing an ender pearl (-0.3 = 30% shorter)</li>
 *   <li>{@code spell_ward}: a <i>chance</i> that a harmful effect or a magic attack from someone else is cancelled</li>
 *   <li>{@code no_trade}: any amount above 0 = villagers and wandering traders won't trade with you</li>
 *   <li>{@code first_trade_discount}: your first trade with each villager costs this much less (0.5 = half price)</li>
 *   <li>{@code self_blast_immune}: any amount above 0 = explosions you cause don't hurt you</li>
 *   <li>{@code brewed_duration}: how long potions you take out of a brewing stand last</li>
 *   <li>{@code crop_growth}: how much faster crops within 6 blocks of you grow (0.25 = +25%)</li>
 *   <li>{@code marked_loot}: <i>extra drops</i> from mobs you've marked (scrutinised, challenged...) when you kill them</li>
 *   <li>{@code shield_disable}: how long your shield stays disabled after an axe hit (-1 = never disabled)</li>
 *   <li>{@code quiet_steps}: any amount above 0 = other players and mobs don't hear your footsteps</li>
 *   <li>{@code knockback}: how far your hits knock mobs back</li>
 *   <li>{@code extra_loot} / {@code extra_ore} / {@code extra_crop}: a <i>chance</i> (0.1 = 10%) that a mob you kill drops one
 *       more item / an ore or crop you break drops double</li>
 * </ul>
 */
public record BonusEffect(Kind kind, double amount, List<Scaling> scaling) implements ClassEffect {

    public enum Kind implements StringRepresentable {
        EXPERIENCE, POTION_DURATION, HARMFUL_DURATION, BENEFICIAL_DURATION, APPLIED_DURATION, APPLIED_HARMFUL_DURATION, HUNGER, TRADE_PRICE, DRAW_SPEED, RELOAD_SPEED, RESOURCE_GAIN, ABILITY_COST, NATURAL_REGEN, DETECTION_RANGE, SKILL_TRAINING, KNOCKBACK, EXTRA_LOOT, EXTRA_ORE, EXTRA_CROP, ABILITY_COOLDOWN, DURABILITY, ARMOR_DURABILITY, WEAPON_DURABILITY, WEAPON_REPAIR, ARMOR_FLOOR, NO_SPRINT, HEALING_RECEIVED, TEMPLE_RADIUS, HEALING_GIVEN, AFFINITY_FLOOR, OFFHAND_SWING, NO_SHIELD,
        FOOD_SAFE, NO_TRAMPLE, ARMOR_PIERCE, RANGED_ARMOR_PIERCE, PROJECTILE_SPEED, EXTRA_ARROW, WITCH_WARD,
        NO_EATING_IN_COMBAT, ANVIL_SAFE,
        ANVIL_COST, ANVIL_LEVELS, ENCHANT_LEVELS, ENCHANT_LAPIS,
        LOOTING, DRUNK_POTION_DURATION,
        BONE_MEAL, BREEDING_SPEED, ARROW_PIERCE, PEARL_RANGE, MIN_MELEE_DAMAGE, NO_SLEEP,
        PEARL_COOLDOWN, SPELL_WARD, NO_TRADE, FIRST_TRADE_DISCOUNT, SELF_BLAST_IMMUNE,
        BREWED_DURATION, CROP_GROWTH, MARKED_LOOT, SHIELD_DISABLE, QUIET_STEPS;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<BonusEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(BonusEffect::kind),
            Codec.DOUBLE.fieldOf("amount").forGetter(BonusEffect::amount),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(BonusEffect::scaling)
    ).apply(i, BonusEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.BONUS;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addBonus(kind, Scaling.evaluate(amount, scaling, ctx) * ctx.rank());
    }
}

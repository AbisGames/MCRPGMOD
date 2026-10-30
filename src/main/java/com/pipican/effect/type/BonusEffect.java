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
 *   <li>{@code crafted_durability}: tools and weapons you craft get this much more durability (0.05 = +5%)</li>
 *   <li>{@code sun_burn}: any amount above 0 = you catch fire in daylight under the open sky without a helmet, like a zombie</li>
 *   <li>{@code no_beacon}: any amount above 0 = beacon (ambient) effects don't reach you</li>
 *   <li>{@code no_phantoms}: any amount above 0 = phantoms never spawn for you</li>
 *   <li>{@code extra_enchant}: a <i>chance</i> that enchanting at a table adds one more enchantment</li>
 *   <li>{@code magic_portion}: the share of your melee hits' damage dealt again as magic damage (ignores armor)</li>
 *   <li>{@code splash_undead}: <i>damage</i> your splash and lingering potions deal to undead within 4 blocks of where they break</li>
 *   <li>{@code thrown_speed}: how fast your thrown potions and fire charges fly</li>
 *   <li>{@code max_wolves}: above 0 = you can't tame a wolf while this many (rounded) of yours are alive and loaded</li>
 *   <li>{@code open_locks}: a <i>chance</i> that opening a loot chest no one has opened yet adds one extra roll of its loot</li>
 *   <li>{@code redirect_hit}: a <i>chance</i> that a mob's hit on you lands on a random other mob within 6 blocks instead</li>
 *   <li>{@code ill_fortune}: a <i>chance</i> that a hostile mob within 8 blocks of you misses whoever it attacks</li>
 *   <li>{@code brew_speed}: how much faster brewing stands you last opened brew (0.25 = +25%)</li>
 *   <li>{@code no_top_tier}: any amount above 0 = your skills stop just short of their last tier</li>
 *   <li>{@code resource_refund}: a <i>chance</i> that what an ability cost comes straight back;
 *       {@code refund_bonus}: how much more that refund gives (0.5 = 150%), up to your bar's maximum</li>
 *   <li>{@code share_drink}: any amount above 0 = effects from potions you drink also go to your nearest tamed animal within 16 blocks</li>
 *   <li>{@code free_ability_every}: <i>a count</i> - every that-many-th ability you use costs nothing (3 = every third)</li>
 *   <li>{@code weather_warning}: any amount above 0 = a chat message 30s before rain or a thunderstorm starts</li>
 *   <li>{@code glorious_death}: any amount above 0 = once per day, a killing blow leaves you at 1 HP for 5s, then you die</li>
 *   <li>{@code guard_allies}: the share of the damage other players within 8 blocks take that you take instead</li>
 *   <li>{@code projectile_ward}: any amount above 0 = projectiles flying at your front don't hurt players within 3 blocks behind you</li>
 *   <li>{@code execute_looting}: <i>Looting levels</i> added when a mob you execute dies</li>
 *   <li>{@code sure_tame}: any amount above 0 = wolves, cats and parrots are tamed by the first bone, fish or seeds</li>
 *   <li>{@code fire_charge_damage}: how much damage fire charges / fireballs you own deal;
 *       {@code wind_charge_damage}: <i>damage</i> added to direct hits of your wind charges</li>
 *   <li>{@code distance_damage}: extra projectile damage per full 16 blocks between you and the target (0.1 = +10%)</li>
 *   <li>{@code triage}: how much more your heals heal other players below 30% health (1 = twice as much)</li>
 *   <li>{@code offhand_combo}: how much more your off-hand swing deals right after a main-hand hit on the same mob</li>
 *   <li>{@code scrutiny_expose}: how much more damage scrutinised mobs take from everyone (0.1 = +10%)</li>
 *   <li>{@code scrutiny_weakness}: <i>blocks</i> - mobs you've scrutinised within this range get Weakness I</li>
 *   <li>{@code keep_hotbar_weapon}: any amount above 0 = the item in hotbar slot 1 stays with you when you die</li>
 *   <li>{@code grindstone_xp}: how much more experience disenchanting at a grindstone gives (0.1 = +10%)</li>
 *   <li>{@code keep_optimization}: any amount above 0 = Optimization isn't lost when you die</li>
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
        BREWED_DURATION, CROP_GROWTH, MARKED_LOOT, SHIELD_DISABLE, QUIET_STEPS,
        CRAFTED_DURABILITY, SUN_BURN, NO_BEACON, NO_PHANTOMS, EXTRA_ENCHANT, MAGIC_PORTION,
        SPLASH_UNDEAD, THROWN_SPEED, MAX_WOLVES, OPEN_LOCKS, REDIRECT_HIT, ILL_FORTUNE, BREW_SPEED, NO_TOP_TIER,
        RESOURCE_REFUND, REFUND_BONUS, SHARE_DRINK,
        FREE_ABILITY_EVERY, WEATHER_WARNING, GLORIOUS_DEATH, GUARD_ALLIES, PROJECTILE_WARD, EXECUTE_LOOTING, SURE_TAME,
        FIRE_CHARGE_DAMAGE, WIND_CHARGE_DAMAGE, DISTANCE_DAMAGE, TRIAGE, OFFHAND_COMBO,
        SCRUTINY_EXPOSE, SCRUTINY_WEAKNESS, KEEP_HOTBAR_WEAPON, GRINDSTONE_XP, KEEP_OPTIMIZATION;

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

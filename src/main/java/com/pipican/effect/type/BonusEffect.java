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
 *   <li>{@code explode_self_share}: the share of the damage your own {@code pipican:explode} blasts deal that you take too</li>
 *   <li>{@code empower_charges}: <i>extra charges</i> of {@code pipican:empower} with {@code charges} you can store (whole numbers)</li>
 *   <li>{@code wildfire}: any amount above 0 = burning mobs you hurt set mobs next to them on fire</li>
 *   <li>{@code taunt_radius}: <i>blocks</i> - hostile mobs this close that target another player switch to you</li>
 *   <li>{@code quarry_pursuit}: movement speed while you move toward one of your quarries (0.1 = +10%)</li>
 *   <li>{@code quarry_attack_speed} / {@code quarry_reach}: attack speed (0.05 = +5%) / reach (<i>blocks</i>) while you look
 *       at one of your quarries</li>
 *   <li>{@code unmarked_forbidden}: any amount above 0 = weapons and armor that aren't sigil-marked are forbidden-tier for you</li>
 *   <li>{@code staff_only}: any amount above 0 = weapons other than a staff are at best clumsy for you</li>
 *   <li>{@code favored_only}: any amount above 0 = weapons that aren't your favored kind are forbidden-tier for you</li>
 *   <li>{@code golem_chest}: any amount above 0 = iron golems that see you open a chest in a village turn on you</li>
 *   <li>{@code animals_flee} / {@code animals_follow}: any amount above 0 = passive animals within 10 blocks run from you /
 *       follow you</li>
 *   <li>{@code burn_longer}: how much longer fire lasts on you once you catch it (0.5 = +50%)</li>
 *   <li>Losing mobs: a mob that can't see you forgets you after 3s; {@code forget_faster} shortens that (0.5 = half as
 *       long), {@code forget_sooner} by <i>seconds</i>, {@code forget_delay_max} caps it (<i>seconds</i>), and
 *       {@code forget_hit_sneaking} shortens it more for mobs you hit while sneaking</li>
 *   <li>{@code faceless_range}: <i>blocks</i> - while sneaking and not attacking, hostile mobs farther than this can't target
 *       you (0 or less: off)</li>
 *   <li>{@code pet_share_sight}: any amount above 0 = your tamed animals within 32 blocks get your Night Vision and glow</li>
 *   <li>{@code no_hiding}: <i>blocks</i> - invisible mobs this close that you can see lose their invisibility</li>
 *   <li>{@code danger_sense}: <i>blocks</i> - a sound cue when a hostile mob comes this close</li>
 *   <li>{@code never_unaware}: any amount above 0 = "from behind" / "not targeting you" bonuses never apply against you</li>
 *   <li>{@code bookshelf_bonus}: <i>extra bookshelves</i> your enchanting table counts (up to the usual 15)</li>
 *   <li>{@code enchant_lapis_keep}: a <i>chance</i> that an enchant gives back all the lapis it cost;
 *       {@code enchant_lapis_top}: <i>lapis</i> back on the level-3 option</li>
 *   <li>{@code better_roll}: a <i>chance</i> that each enchanting option is offered about 20% higher (at least +1 level)</li>
 *   <li>{@code chaos_luck}: a <i>chance</i> that a random outcome which hurts you is rolled again</li>
 *   <li>{@code trample}: <i>damage</i> your mount deals to small mobs it runs into (once a second each)</li>
 *   <li>{@code illager_detection}: how far illagers notice you (-0.3 = 30% closer)</li>
 *   <li>{@code fall_floor}: <i>health</i> a fall can't take you below; {@code fall_safe_ground}: no fall damage landing on stone or dirt</li>
 *   <li>{@code wind_knockback}: how far your wind charges throw others (0.5 = +50%)</li>
 *   <li>{@code save_ally}: once per day, another player who dies within 16 blocks of you is saved at this much <i>health</i></li>
 *   <li>{@code decoy_proof}: any amount above 0 = wardens, endermen and the Wither are never fooled by your decoys and dodges</li>
 *   <li>{@code no_metal_armor}: any amount above 0 = iron and netherite armor is forbidden-tier for you</li>
 *   <li>{@code always_dark}: any amount above 0 = you count as being in darkness for light-level conditions</li>
 *   <li>{@code reoptimize} / {@code swap_favored}: once per day you may re-pick Optimization / your favored weapon without sleeping</li>
 *   <li>{@code low_health_extra}: <i>health</i> added to the "below 2 hearts" line of low-health reactions</li>
 *   <li>{@code tnt_power}: how much stronger TNT you light explodes (0.25 = +25%)</li>
 *   <li>{@code charges_fizzle_nether}: any amount above 0 = stored empower charges aren't gained in the Nether</li>
 *   <li>{@code poison_brew_speed}: how much faster brewing stands you opened brew spider-eye (poison) potions</li>
 *   <li>{@code fire_longer}: how much longer fires you start on mobs burn (0.5 = +50%)</li>
 *   <li>{@code marked_weakness}: per whole point, mobs you've marked within 4 blocks get Weakness (1 = I, 2 = II)</li>
 *   <li>{@code extra_restock}: how much more often villagers you trade with restock (1 = twice as often)</li>
 *   <li>{@code pet_regen}: <i>health</i> your tamed animals within 32 blocks regain every 5 seconds</li>
 *   <li>{@code aura_break_at}: <i>damage</i> - a hit bigger than this switches your auras off for 10s</li>
 *   <li>{@code calm_animals}: any amount above 0 = animals you hurt don't panic and run</li>
 *   <li>{@code shield_break_time}: how much longer a shield you disable with an axe stays down (0.1 = +10%)</li>
 *   <li>{@code splash_power}: how much stronger the Instant Health / Instant Damage of splash potions you throw is</li>
 *   <li>{@code extra_brew}: a <i>chance</i> that a brewing stand you opened brews one extra potion</li>
 *   <li>{@code sense_magic} / {@code loot_sense} / {@code ore_sense}: <i>blocks</i> - while sneaking, enchanted items and mobs
 *       with effects glow / unopened loot chests / ores sparkle for you</li>
 *   <li>{@code structure_sense}: <i>blocks</i> - holding a compass names the structures this close</li>
 *   <li>{@code prey_reveal}: per whole point, your quarries can't stay invisible</li>
 *   <li>{@code emit_light}: the <i>light level</i> (up to 15) you give off where you stand</li>
 *   <li>{@code staff_return}: any amount above 0 = a staff you drop (or lose on death) comes back to your inventory</li>
 *   <li>{@code book_hotbar}: any amount above 0 = a book anywhere in your hotbar counts as held in the off-hand</li>
 *   <li>{@code pearl_carry}: any amount above 0 = your ender pearls also carry the nearest player within 3 blocks</li>
 *   <li>{@code mount_return}: any amount above 0 = a tamed mount of yours that dies comes back at your respawn point a day later</li>
 *   <li>{@code sweep_execute}: <i>health share</i> - mobs your sweeping attack hits below it die</li>
 *   <li>Firearms (crossbows stand in): {@code accuracy} pulls shots toward where you aim (negative: more spread);
 *       {@code shot_alert} (<i>blocks</i>) - hostile mobs that close come for you when you fire; {@code quick_draw} - the first
 *       shot within 5s of drawing deals more; {@code cylinder} (<i>shots</i>) - the crossbow reloads itself from your arrows until
 *       that many shots are fired; {@code last_shot_crit} - the last shot of a cylinder is critical;
 *       {@code ricochet} - the share of a shot's damage that bounces to another mob within 4 blocks</li>
 *   <li>{@code threshold_drop}: how much lower (0.25 = 25 points of the bar) "lowerable" resource conditions start</li>
 *   <li>{@code temple_alarm}: any amount above 0 = Speed II for 2s when a hostile mob comes into your temple</li>
 *   <li>{@code stance_walk}: any amount above 0 = sneaking while walking still counts as standing still</li>
 *   <li>{@code chaos_double}: any amount above 0 = random outcomes on a mob also hit the nearest other mob within 4 blocks</li>
 *   <li>Movement (any amount above 0): {@code no_cobweb} / {@code no_powder_snow} - they don't slow you; {@code double_jump} -
 *       jump again in mid-air (once per 5s); {@code wall_run} - sprinting into a wall runs up it 2 blocks; {@code wall_climb} -
 *       sneak and hold jump to climb walls; {@code glide} - hold jump while falling to glide</li>
 *   <li>{@code hide_nameplate}: any amount above 0 = other players don't see your name while you sneak or are invisible</li>
 *   <li>{@code icon_shield}: any amount above 0 = a shield with a banner pattern in the off-hand counts as your Icon of Faith</li>
 *   <li>{@code puppet}: any amount above 0 = the first mob you charm becomes your ally until you sleep (one at a time)</li>
 *   <li>{@code poison_undead}: any amount above 0 = Poison you give an undead mob becomes Wither instead; {@code poison_damage}:
 *       how much more damage Poison (and that Wither) you gave does</li>
 *   <li>{@code side_block}: any amount above 0 = a raised shield also blocks projectiles from the sides (not from behind)</li>
 *   <li>{@code sink}: any amount above 0 = you sink in water and can't swim upward</li>
 *   <li>{@code copper_heart}: how much faster copper golems within 16 blocks move (0.25 = +25%)</li>
 *   <li>{@code quarry_double}: quarry bonuses (damage to marked mobs, a quarry's own damage and others-damage cut, quarry pursuit /
 *       attack speed / reach) are this much stronger (1 = doubled); {@code quarry_lockout}: for this many seconds after picking a
 *       quarry, you can't damage other mobs</li>
 *   <li>{@code mark_targets}: how many more mobs a capped mark ({@code max} on {@code pipican:mark}) may be on at once;
 *       {@code scrutiny_targets}: how many more mobs can be scrutinised at once (one without it - a new one replaces the oldest)</li>
 *   <li>{@code empower_knockback}: the first hit of each "your next hits" buff also knocks the mob back this hard (vanilla
 *       Knockback I is about 0.5)</li>
 *   <li>Temple: {@code temple_mobile} - your temple follows you, this many blocks wide; {@code temple_strength} - bonuses that
 *       need you in your temple are this much stronger (1 = doubled, -0.15 = 15% weaker); {@code temple_only_passives} (any amount
 *       above 0) - outside your temple your class passives don't work; {@code temple_healing} - players in your temple heal this
 *       much more (0.2 = +20%)</li>
 *   <li>{@code wandering_trades}: whole extra offers a wandering trader rolls when you talk to it (once per trader; everyone sees them)</li>
 *   <li>{@code drunk_potion_strength}: potions you drink are this much stronger (-0.3 = 30% weaker: levels round to the nearest,
 *       never below I; Instant Health heals 30% less)</li>
 *   <li>{@code magic_armor_pierce}: the share of armor your magic damage ignores</li>
 *   <li>{@code bare_handed}: any amount above 0 = your hits only count without an item in hand, and items can't be used</li>
 *   <li>{@code focus_slots}: <i>focus schools</i> you can pick ({@code /pipican focus}); {@code focus_bonus} / {@code non_focus_bonus}:
 *       how much more damage (and Life healing) the picked / other schools give</li>
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
        SCRUTINY_EXPOSE, SCRUTINY_WEAKNESS, KEEP_HOTBAR_WEAPON, GRINDSTONE_XP, KEEP_OPTIMIZATION, EXPLODE_SELF_SHARE,
        EMPOWER_CHARGES, WILDFIRE, TAUNT_RADIUS, QUARRY_PURSUIT, QUARRY_ATTACK_SPEED, QUARRY_REACH,
        UNMARKED_FORBIDDEN, STAFF_ONLY, FAVORED_ONLY,
        GOLEM_CHEST, ANIMALS_FLEE, ANIMALS_FOLLOW, BURN_LONGER,
        FORGET_FASTER, FORGET_DELAY_MAX, FORGET_SOONER, FORGET_HIT_SNEAKING, FACELESS_RANGE, PET_SHARE_SIGHT, NO_HIDING,
        DANGER_SENSE, NEVER_UNAWARE,
        BOOKSHELF_BONUS, ENCHANT_LAPIS_KEEP, ENCHANT_LAPIS_TOP, BARE_HANDED, FOCUS_SLOTS, FOCUS_BONUS, NON_FOCUS_BONUS, BETTER_ROLL,
        CHAOS_LUCK, TRAMPLE, ILLAGER_DETECTION,
        FALL_FLOOR, FALL_SAFE_GROUND, WIND_KNOCKBACK, SAVE_ALLY, DECOY_PROOF, NO_METAL_ARMOR, ALWAYS_DARK, REOPTIMIZE, SWAP_FAVORED,
        LOW_HEALTH_EXTRA, TNT_POWER, CHARGES_FIZZLE_NETHER, POISON_BREW_SPEED, FIRE_LONGER, MARKED_WEAKNESS, EXTRA_RESTOCK, PET_REGEN,
        AURA_BREAK_AT, CALM_ANIMALS, SHIELD_BREAK_TIME, SPLASH_POWER, EXTRA_BREW, SENSE_MAGIC, LOOT_SENSE, ORE_SENSE, STRUCTURE_SENSE, PREY_REVEAL,
        EMIT_LIGHT, STAFF_RETURN, BOOK_HOTBAR, PEARL_CARRY, MOUNT_RETURN, SWEEP_EXECUTE,
        ACCURACY, SHOT_ALERT, QUICK_DRAW, CYLINDER, LAST_SHOT_CRIT, RICOCHET,
        THRESHOLD_DROP, TEMPLE_ALARM, STANCE_WALK, CHAOS_DOUBLE,
        NO_COBWEB, NO_POWDER_SNOW, DOUBLE_JUMP, WALL_RUN, WALL_CLIMB, GLIDE,
        HIDE_NAMEPLATE, ICON_SHIELD, PUPPET,
        POISON_UNDEAD, POISON_DAMAGE, SIDE_BLOCK, SINK, COPPER_HEART, MAGIC_ARMOR_PIERCE, QUARRY_DOUBLE, QUARRY_LOCKOUT, MARK_TARGETS, SCRUTINY_TARGETS, EMPOWER_KNOCKBACK,
        TEMPLE_MOBILE, TEMPLE_STRENGTH, TEMPLE_ONLY_PASSIVES, TEMPLE_HEALING, WANDERING_TRADES, DRUNK_POTION_STRENGTH;

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

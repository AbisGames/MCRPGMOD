"""Gear sets: one per set ore (and the T8/T9 and alloy sets). Each set gets a helmet, chestplate, leggings, boots,
sword, axe, spear, pickaxe, shovel and hoe (Valyrian steel: weapons only).

Fields:
  style   the stat profile (see STYLES): how armor, speed, toughness and durability lean
  armor   bonuses while wearing all four armor pieces
  weapon  bonuses while holding one of the set's weapons or tools
  helmet  bonuses while wearing just the helmet
  traits  item traits: unbreakable, self_repair, autosmelt, area_mine (3x3), soulbound (kept on death),
          mine_all (mines every ore), fragile (half durability), durable (double durability)
Bonuses are written like class-spec lines ("+20% damage against undead") and translated by the class builder, or
given as raw effect JSON (dicts) where no wording exists. Within a tier the sets are balanced to the same total: a
faster set protects less, a tank set moves slower.
"""

# Stat profile per style: armor multiplier, extra toughness, extra knockback resistance, mining speed multiplier,
# sword attack speed bonus, enchantability bonus, armor class tag (light / medium / heavy).
STYLES = {
    'balanced': dict(armor=1.0, tough=0.0, kb=0.0, speed=1.0, swing=0.0, ench=0, weight='medium'),
    'fast': dict(armor=0.8, tough=0.0, kb=0.0, speed=1.3, swing=0.3, ench=2, weight='light'),
    'light': dict(armor=0.85, tough=0.0, kb=0.0, speed=1.1, swing=0.2, ench=4, weight='light'),
    'magic': dict(armor=0.85, tough=0.0, kb=0.0, speed=1.0, swing=0.0, ench=10, weight='light'),
    'ranged': dict(armor=0.85, tough=0.0, kb=0.0, speed=1.0, swing=0.1, ench=4, weight='light'),
    'crit': dict(armor=0.95, tough=0.0, kb=0.0, speed=1.0, swing=0.1, ench=0, weight='medium'),
    'sustain': dict(armor=1.0, tough=0.0, kb=0.0, speed=1.0, swing=0.0, ench=2, weight='medium'),
    'tank': dict(armor=1.15, tough=1.0, kb=0.1, speed=0.9, swing=-0.2, ench=0, weight='heavy'),
    'heavy': dict(armor=1.2, tough=1.5, kb=0.15, speed=0.85, swing=-0.3, ench=-2, weight='heavy'),
}


def _bonus(kind, amount):
    return {'type': 'pipican:bonus', 'kind': kind, 'amount': amount}


def _effect(effect, amplifier=0):
    e = {'type': 'pipican:mob_effect', 'effect': 'minecraft:' + effect}
    if amplifier:
        e['amplifier'] = amplifier
    return e


def _on_hurt(actions, cooldown_ticks):
    return {'type': 'pipican:on', 'event': 'hurt', 'cooldown_ticks': cooldown_ticks, 'actions': actions}


def _taken(tag, mult):
    return {'type': 'pipican:damage_taken', 'damage_types': tag, 'multiplier': mult}


def _vs_only(tag, mult):
    """Damage to anything that isn't in the mob tag is multiplied by `mult` ("hurts only monsters")."""
    return {'type': 'pipican:damage_dealt', 'against': tag, 'except': True, 'multiplier': mult}


def _s(style, armor=(), weapon=(), helmet=(), traits=()):
    return {'style': style, 'armor': list(armor), 'weapon': list(weapon), 'helmet': list(helmet), 'traits': list(traits)}


SETS = {
    # ---------------------------------------------------------------- T1
    'electrum': _s('magic', ['+10% magic damage', '+5% movement speed'], ['+15% mining speed'], traits=['fragile']),
    'cold_iron': _s('balanced', ['harmful effects on you last 25% shorter'], ['+50% damage against fey and spirits']),
    'truesilver': _s('balanced', ['-10% damage taken from undead'],
                     ['+25% damage against undead'], helmet=['night vision']),
    'corundum': _s('balanced', ['+1 armor toughness', '+5% damage'], ['+5% attack speed']),
    'stormsilver': _s('fast', ['+5% movement speed', {'type': 'pipican:conditional', 'when': {'type': 'pipican:any_of', 'conditions': [
        {'type': 'pipican:state', 'state': 'raining'}, {'type': 'pipican:state', 'state': 'thundering'}]},
        'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:attack_speed', 'amount': 0.15, 'operation': 'add_multiplied_base'}]}],
                      ['+10% attack speed']),
    'runic_iron': _s('magic', ['+8% magic damage', '+1 armor'], ['+5% damage']),
    'redrock_iron': _s('tank', ['+2 max health', '-10% fire damage taken'], ['+5% damage']),
    'pearlsilver': _s('light', ['+20% swim speed', 'water breathing'], ['+15% damage while in water']),

    # ---------------------------------------------------------------- T2
    'moonstone': _s('fast', ['+10% damage at night', '+10% movement speed at night'], ['+10% damage at night']),
    'demonite': _s('crit', ['+8% crit chance', 'when you take damage, become invisible for 2s (30s cooldown)'], ['+15% critical damage']),
    'crimtane': _s('sustain', ['+2 max health'], ['your hits have a 15% chance to heal you 2 HP']),
    'machalite': _s('balanced', ['+1 armor toughness'], ['+20% damage against bosses', '+15% damage against large creatures']),
    'glacierite': _s('tank', ['immune to freezing damage', '-10% damage taken'], ['mobs you hit get Slowness I for 2s']),
    'cinderite': _s('balanced', ['fire resistance'], ['your hits set mobs on fire for 2s']),
    'tidalite': _s('light', ['+25% swim speed', '+10% damage while in water'], ['+15% damage while in water']),

    # ---------------------------------------------------------------- T3
    'cobalt': _s('fast', ['+10% movement speed'], ['+40% mining speed']),
    'ardite': _s('heavy', ['fire resistance', '-10% movement speed'], ['+10% damage'], traits=['durable']),
    'thorium': _s('heavy', ['immune to knockback', '-10% movement speed'], ['+10% damage']),
    'malachite': _s('light', ['12% chance to dodge attacks'], ['+20% damage'], traits=['fragile']),
    'quicksilver': _s('fast', ['+15% movement speed', '+10% attack speed'], ['+10% attack speed']),
    'hellstone': _s('balanced', ['immune to fire damage'], ['your hits set mobs on fire for 4s', '+10% fire damage']),
    'fel_iron': _s('crit', ['you take 1 damage every 10s', '+15% damage'], ['your hits set mobs on fire for 3s', '+20% fire damage']),
    'dwemer_metal': _s('tank', [_taken('#minecraft:is_explosion', 0.7), '-20% projectile damage taken'], ['+10% damage'], traits=['autosmelt']),
    'thaumium': _s('magic', ['+15% magic damage', _bonus('ability_cost', -0.15)], ['+10% magic damage']),
    'manasteel': _s('magic', ['+10% magic damage', _bonus('ability_cooldown', -0.1)], ['+10% mining speed'], traits=['self_repair']),
    'wintersteel': _s('balanced', ['immune to freezing damage', '+1 armor toughness'], ['mobs you hit get Slowness I for 3s', '+15% freezing damage']),
    'royal_silver': _s('balanced', [_bonus('trade_price', -0.1), '-10% damage taken from undead'], ['+30% damage against undead']),
    'skyiron': _s('light', ['+10% movement speed', '+10% mount speed'], ['+15% damage while mounted']),
    'dawnbronze': _s('balanced', ['+15% damage during the day'], ['+15% damage against undead']),

    # ---------------------------------------------------------------- T4
    'meteoric_iron': _s('balanced', ['+1 armor toughness'], ['+30% damage against monsters']),
    'palladium': _s('sustain', ['regeneration I'], ['your hits have a 20% chance to heal you 3 HP']),
    'elementium': _s('magic', ['+15% magic damage'], ['your hits set mobs on fire for 2s', 'mobs you hit get Slowness I for 2s',
                                                       'your hits have a 5% chance to call down lightning on the target']),
    'iridium': _s('tank', ['+2 max health'], ['+20% mining speed'], traits=['area_mine', 'durable']),
    'carbalite': _s('tank', ['-40% damage taken while blocking', '+2 max health'], ['+10% damage']),
    'black_metal': _s('tank', ['-25% damage taken while blocking'], ['+25% damage with axes and maces', '+10% damage']),
    'starsilver': _s('balanced', ['immune to freezing damage'], ['mobs you hit get Slowness II for 2s', '+25% freezing damage']),
    'celestial_bronze': _s('balanced', ['-15% damage taken from undead'], ['+40% damage against monsters', _vs_only('#pipican:mobs/monsters', 0.25)]),
    'imperial_gold': _s('balanced', [{'type': 'pipican:aura', 'name': 'imperial command', 'radius': 8.0, 'include_self': False,
                                      'effects': [{'type': 'pipican:damage_dealt', 'multiplier': 1.1}]}], ['+10% damage']),
    'goblin_silver': _s('balanced', ['+1 armor toughness'], ['mobs you hit get Poison I for 3s'], traits=['self_repair']),
    'dimeritium': _s('tank', ['-80% magic damage taken', _bonus('ability_cost', 1.0), _bonus('ability_cooldown', 1.0)],
                     ['+30% damage against witches', '+20% damage against illagers']),
    'magisteel': _s('magic', [{'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_magic', 'multiplier': 1.0,
                               'scaling': [{'source': 'class_level', 'per': 0.005}]}],
                    [{'type': 'pipican:damage_dealt', 'multiplier': 1.0, 'scaling': [{'source': 'class_level', 'per': 0.004}]}]),

    # ---------------------------------------------------------------- T5
    'mithril': _s('heavy', ['+15% movement speed', '+2 armor'], ['+15% damage']),
    'ebony': _s('crit', ['+25% stealth'], ['+25% damage']),
    'stalhrim': _s('magic', ['immune to freezing damage', '+20% freezing damage'], ['mobs you hit get Slowness III for 2s', '+20% freezing damage']),
    'eternium': _s('balanced', ['your gear loses durability 80% slower', '+1 armor toughness'], ['+15% damage'], traits=['durable']),
    'saronite': _s('crit', ['+30% damage', 'hostile mobs notice you 50% farther away'], ['+15% damage']),
    'runite': _s('balanced', ['+15% damage', '+1 armor toughness'], ['+15% damage']),
    'titanium': _s('tank', ['+4 armor for 3s after you take damage'], ['+15% damage']),
    'hallowed': _s('balanced', ['Resistance IV for 1s after you take damage'], ['+35% damage against undead']),
    'stygian_iron': _s('crit', ['every kill refills 5% health'], ['+25% damage', 'mobs you hit get Wither I for 2s']),
    'luna_titanium': _s('heavy', ['immune to knockback', '-15% movement speed', '+4 max health'], ['+15% damage']),
    'dragonite': _s('balanced', ['fire resistance'], ['+40% damage against dragons', '+20% damage against end mobs']),
    'scarlet': _s('crit', [], ['+35% damage against demons and undead', '+10% critical damage']),
    'crystallite': _s('fast', ['+10% movement speed'], [{'type': 'pipican:nth_hit', 'every': 3, 'multiplier': 1.4}]),
    'gromril': _s('heavy', ['+3 armor', '-10% movement speed'], ['+15% damage']),
    'ithilmar': _s('ranged', ['+15% movement speed'], ['+25% damage with bows', '+20% arrow speed']),
    'carbonadium': _s('tank', ['+2 armor toughness'], ['mobs you hit get Weakness I for 3s'], traits=['durable']),
    'umbrite': _s('crit', ['+25% stealth', '+10% damage at night'], ['+20% damage at night']),

    # ---------------------------------------------------------------- T6
    'orichalcum': _s('balanced', ['water breathing', '+15% magic damage'], ['+20% damage']),
    'chlorophyte': _s('sustain', ['regeneration I'], ['+25% damage with bows', '+15% damage']),
    'aetherium': _s('magic', ['+30% magic damage', _bonus('ability_cost', -0.15)], ['+15% magic damage']),
    'void_metal': _s('crit', ['+20% damage'], ['mobs you hit get Wither II for 3s'], traits=['self_repair']),
    'gundanium': _s('heavy', ['hostile mobs notice you 40% later', '+4 max health'], ['+15% damage'], traits=['durable']),
    'inertron': _s('heavy', ['immune to knockback', '-20% damage taken'], ['+15% damage']),
    'sigmarite': _s('tank', ['-80% damage taken from undead', '-60% damage taken from nether mobs'], ['+30% damage against demons and undead']),
    'phrik': _s('tank', ['-40% magic damage taken', '-20% projectile damage taken'], ['+20% damage']),
    'trinium': _s('light', ['+20% movement speed', '+2 armor toughness'], ['+20% damage']),
    'dragon_metal': _s('crit', ['+15% critical damage'], [{'type': 'pipican:nth_hit', 'every': 4, 'multiplier': 1.6, 'strike_twice': True}]),
    'flametal': _s('balanced', ['immune to fire damage', 'fire resistance'], ['your hits set mobs on fire for 5s', '+25% fire damage']),
    'allthemodium': _s('balanced', ['+20% damage', '+2 armor toughness'], ['+30% mining speed']),

    # ---------------------------------------------------------------- T7
    'adamantine': _s('tank', ['+4 max health', '-15% damage taken'], ['+20% damage'], traits=['unbreakable', 'mine_all']),
    'hihiirokane': _s('balanced', ['immune to fire damage', 'you glow and light the area'], ['your hits set mobs on fire for 5s', '+35% fire damage']),
    'darksteel': _s('balanced', ['+25% damage'], ['+15% damage'], traits=['unbreakable']),
    'beskar': _s('tank', ['-50% magic damage taken', '-30% projectile damage taken'], ['+20% damage'], traits=['soulbound']),
    'nth_metal': _s('light', ['no fall damage', _effect('slow_falling')], ['+25% damage']),
    'vibranium': _s('tank', ['-25% damage taken', _on_hurt([{'type': 'pipican:push', 'target': 'hostiles', 'radius': 5.0, 'strength': 1.5}], 100)],
                    ['+20% damage']),

    # ---------------------------------------------------------------- T8 (limited; phase 4 makes them obtainable)
    'uru': _s('crit', ['+30% damage'], ['your hits have a 20% chance to call down lightning on the target', '+20% damage']),
    'adamantium': _s('heavy', ['-35% damage taken', 'immune to knockback'], ['+25% damage'], traits=['unbreakable']),
    'draconium': _s('balanced', [{'type': 'pipican:flight'}, _on_hurt([{'type': 'pipican:absorb', 'amount': 8, 'seconds': 20}], 400)], ['+30% damage']),
    'luminite': _s('balanced', ['+25% damage', '+20% magic damage', '+15% movement speed'], ['+25% damage']),
    'phazon': _s('crit', ['+50% damage', 'you take 1 damage every 10s'], ['+30% damage', 'mobs you hit get Wither II for 3s']),

    # ---------------------------------------------------------------- T9
    'unobtanium': _s('balanced', ['+40% damage', '-30% damage taken', '+20% movement speed', 'regeneration I'], ['+40% damage'],
                     traits=['unbreakable', 'mine_all', 'soulbound']),

    # ---------------------------------------------------------------- alloys
    'arcanite': _s('magic', ['+25% magic damage'], ['+15% magic damage']),
    'manyullyn': _s('crit', ['+10% crit chance'], ['+40% critical damage']),
    'terrasteel': _s('balanced', ['regeneration I'], [{'type': 'pipican:cleave', 'fraction': 0.4, 'radius': 2.5, 'max_targets': 6, 'charged_only': True}]),
    'daedric': _s('crit', ['+40% damage', '-6 max health'], ['+25% damage']),
    'valyrian_steel': _s('fast', [], ['+30% damage', '+40% damage against undead']),
}

# Sets with no armor or tools - weapons only.
WEAPONS_ONLY = {'valyrian_steel'}

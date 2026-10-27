"""Hand translations of the abilities described in class_specs (section 12 and "unlocks X ability").

Each entry maps (class id, ability name as written in the spec) to an ability definition. The builder
writes them to data/pipican/pipican/ability/<class>/<name>.json, gives each class the abilities that
no node unlocks, and adds "grants" to the nodes that do. An ability missing here is listed in the
build report as not in the game yet.

`approx` is a note for the report when the ability only approximates what the spec describes.
Costs are in the class's own resource; cooldowns are in seconds here (converted to ticks).
"""

ONCE_PER_REST = 300  # seconds; "once per rest/sleep" until rest tracking exists


def E(effect, seconds, amplifier=0):
    e = {'effect': 'minecraft:' + effect, 'seconds': seconds}
    if amplifier:
        e['amplifier'] = amplifier
    return e


def effect(target, *effects, **kw):
    return dict({'type': 'pipican:effect', 'target': target, 'effects': list(effects)}, **kw)


def stun(seconds):
    return E('slowness', seconds, 9)


def heal(target='self', amount=0, percent=0, **kw):
    d = {'type': 'pipican:heal', 'target': target}
    if amount:
        d['amount'] = amount
    if percent:
        d['percent'] = percent
    return dict(d, **kw)


def damage(amount, target='hostiles', **kw):
    return dict({'type': 'pipican:damage', 'target': target, 'amount': amount}, **kw)


def bolt(amount, range_=24, **kw):
    return damage(amount, 'look', range=range_, **kw)


def push(strength, radius=6, **kw):
    return dict({'type': 'pipican:push', 'target': 'hostiles', 'radius': radius, 'strength': strength}, **kw)


def dash(strength, up=0.2):
    return {'type': 'pipican:dash', 'strength': strength, 'up': up}


def cleanse(target='self', effects=None, **kw):
    d = {'type': 'pipican:cleanse', 'target': target}
    if effects:
        d['effects'] = ['minecraft:' + e for e in effects]
    return dict(d, **kw)


def attr(attribute, amount, op=None):
    d = {'type': 'pipican:attribute', 'attribute': 'minecraft:' + attribute, 'amount': amount}
    if op:
        d['operation'] = op
    return d


def dealt(mult, weapons=None):
    d = {'type': 'pipican:damage_dealt', 'multiplier': mult}
    if weapons:
        d['weapons'] = weapons
    return d


STAFF = {'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}

A = {
    ('abjurer', 'Ward'): dict(icon='shield', cost=20, cooldown=15, actions=[{'type': 'pipican:absorb', 'amount': 8, 'seconds': 30}]),
    ('abjurer', 'Sanctuary'): dict(icon='end_crystal', cost=40, cooldown=40, actions=[
        push(1.8, 4), effect('hostiles', E('slowness', 5, 3), radius=4)],
        approx='pushes mobs out of 4 blocks once and slows them, instead of a 5s barrier'),
    ('acrobat', 'Dash'): dict(icon='feather', cost=10, cooldown=3, actions=[dash(1.4, 0.15)]),
    ('acrobat', 'Vault'): dict(icon='rabbit_foot', cost=15, cooldown=4, actions=[dash(0.8, 0.9)]),
    ('aeromancer', 'Gust'): dict(icon='wind_charge', cost=15, cooldown=4, actions=[push(2.0, 6, cone=90)]),
    ('aeromancer', 'Flight'): dict(icon='elytra', toggle=True, drain=4, cooldown=5, effects=[{'type': 'pipican:flight'}]),
    ('apocalyptist', 'Hasten the Apocalypse'): dict(icon='wither_rose', cost=40, cooldown=25, actions=[
        damage(6, radius=8), effect('hostiles', E('slowness', 6, 2), E('weakness', 6), radius=8)],
        approx='"fall prone" and slowing ground become Slowness III'),
    ('arcanist', 'Arcane Bolt'): dict(icon='amethyst_shard', cost=10, cooldown=1.5, actions=[bolt(5)]),
    ('artificer', 'Mechanical Servants'): dict(icon='copper_block', cost=40, cooldown=90, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:iron_golem', 'count': 1, 'seconds': 30}],
        approx='one iron golem for 30s instead of small copper golems'),
    ('assassin', 'Disguise'): dict(icon='leather_helmet', cost=25, cooldown=30, actions=[effect('self', E('invisibility', 10))],
                                   approx='Invisibility for 10s'),
    ('astromancer', 'Corona Burst'): dict(icon='glowstone_dust', cost=30, cooldown=15, actions=[effect('hostiles', E('blindness', 4), radius=3)]),
    ('astromancer', 'Sunbeam'): dict(icon='blaze_rod', cost=20, cooldown=4, actions=[bolt(7, 32, fire_seconds=2)]),
    ('bard', 'Dirge'): dict(icon='note_block', cost=30, cooldown=20, actions=[effect('hostiles', E('weakness', 8), E('slowness', 8), radius=10)]),
    ('beastmaster', 'Call of the Wild'): dict(icon='bone', cost=40, cooldown=60, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:wolf', 'count': 2, 'seconds': 30}]),
    ('beastmaster', 'Beast Within'): dict(icon='rabbit_hide', cost=20, cooldown=40, duration=20, effects=[
        attr('attack_damage', 2), attr('movement_speed', 0.15, 'add_multiplied_base')],
        approx='+2 attack damage with anything, not only unarmed'),
    ('brute', 'Ground Slam'): dict(icon='anvil', cost=30, cooldown=12, actions=[effect('hostiles', stun(2), radius=3)]),
    ('cavalier', 'Charge'): dict(icon='saddle', cost=25, cooldown=10, actions=[
        dash(1.8, 0.1), damage(8, radius=3, cone=60, kind='melee'), push(1.5, 3, cone=60)],
        approx='hits mobs in front when the charge starts, not on impact'),
    ('chronomancer', 'Haste'): dict(icon='sugar', cost=25, cooldown=30, actions=[effect('self', E('speed', 8, 2), E('haste', 8, 1))]),
    ('chronomancer', 'Time Stop'): dict(icon='clock', cost=60, cooldown=60, actions=[
        effect('hostiles', stun(3), E('weakness', 3, 9), E('mining_fatigue', 3, 9), radius=6)],
        approx='mobs are stunned and harmless for 3s rather than frozen in time'),
    ('death_dealer', 'Whirlwind'): dict(icon='iron_axe', cost=40, cooldown=12, actions=[damage(6, radius=2.5, kind='melee', execute_below=20)]),
    ('dervish', 'Whirl'): dict(icon='iron_sword', cost=25, cooldown=6, actions=[damage(5, radius=3, kind='melee')],
                               approx='one hit on every mob around you (not one per weapon)'),
    ('destroyer', 'Cataclysm'): dict(icon='tnt', cost=60, cooldown=45, actions=[{'type': 'pipican:explode', 'power': 5}]),
    ('diplomat', 'Soothe'): dict(icon='honey_bottle', cost=15, cooldown=5, actions=[
        cleanse('look_ally', ['nausea', 'blindness', 'weakness'], range=8)]),
    ('enchanter', 'Countercharm'): dict(icon='enchanted_book', cost=20, cooldown=15, actions=[cleanse('allies', radius=8)],
                                       approx='removes harmful potion effects (charm and fear don\'t exist yet)'),
    ('exorcist', 'Exorcise'): dict(icon='totem_of_undying', cost=25, cooldown=10, actions=[
        cleanse('look_ally', range=8), effect('look', stun(2), range=8)],
        approx='cleanses the player you look at, or stuns the mob you look at (any mob, not only unclean ones)'),
    ('geomancer', 'Quake'): dict(icon='cobblestone', cost=30, cooldown=15, actions=[damage(3, radius=5, kind='melee'), effect('hostiles', stun(2), radius=5)]),
    ('gladiator', 'Coup de Grace'): dict(icon='netherite_sword', cost=30, cooldown=10, actions=[bolt(10, 4, kind='melee')],
                                       approx='a heavy hit on the mob you look at (any mob, not only affected ones)'),
    ('gunslinger', 'Deadeye'): dict(icon='spyglass', cost=30, cooldown=20, duration=3, effects=[
        dealt(1.5, '#pipican:weapons/crossbows'), dealt(1.5, '#pipican:weapons/bows')],
        approx='+50% ranged damage for 3s instead of slowed time and guaranteed crits'),
    ('healer', 'Heal'): dict(icon='glistering_melon_slice', cost=15, cooldown=4, actions=[heal('look_ally', 8, range=12)]),
    ('healer', 'Mass Heal'): dict(icon='golden_apple', cost=50, cooldown=30, actions=[heal('allies', 6, radius=8)]),
    ('healer', 'Cure'): dict(icon='milk_bucket', cost=20, cooldown=10, actions=[cleanse('look_ally', range=12)]),
    ('hexer', 'Hex'): dict(icon='fermented_spider_eye', cost=20, cooldown=8, actions=[
        effect('look', E('weakness', 10), E('slowness', 10), E('glowing', 10), range=16)],
        approx='a fixed curse (Weakness, Slowness, Glowing) instead of choosing one'),
    ('hydromancer', 'Watery Form'): dict(icon='water_bucket', cost=40, cooldown=40, duration=10, effects=[
        dealt(0.5), {'type': 'pipican:mob_effect', 'effect': 'minecraft:dolphins_grace'},
        {'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'underwater'},
         'effects': [{'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}]}],
        approx='half damage, fast swimming and invisibility underwater; no squeezing through gaps'),
    ('hydromancer', 'Wave'): dict(icon='heart_of_the_sea', cost=25, cooldown=6, actions=[push(2.0, 6)]),
    ('infiltrator', 'Vanish'): dict(icon='ender_eye', cost=30, cooldown=30, actions=[effect('self', E('invisibility', 8))]),
    ('mage_knight', 'Battle Spell'): dict(icon='blaze_powder', cost=20, cooldown=6, actions=[damage(5, radius=4, cone=90), push(1.0, 4, cone=90)]),
    ('magician', 'Spell Recovery'): dict(icon='lapis_lazuli', cooldown=ONCE_PER_REST, actions=[
        {'type': 'pipican:resource', 'percent': 0.5}, heal('self', 4)], approx='refills half the Mana, once per 5 minutes'),
    ('magus', 'Staff Bolt'): dict(icon='blaze_rod', cost=15, cooldown=1.5, requires=STAFF, actions=[bolt(5)]),
    ('marauder', 'Warcry'): dict(icon='goat_horn', cost=30, cooldown=20, actions=[
        effect('hostiles', E('weakness', 8), radius=8), effect('allies', E('speed', 8), radius=8)]),
    ('myrmidon', 'Shield Charge'): dict(icon='shield', cost=25, cooldown=8,
                                        requires={'type': 'pipican:holding', 'items': '#pipican:weapons/shields'},
                                        actions=[dash(1.4, 0.1), push(1.5, 3, cone=90)]),
    ('paladin', 'Lay on Hands'): dict(icon='golden_carrot', cost=15, cooldown=10, actions=[heal('look_ally', 6, range=6)]),
    ('paladin', 'Holy Radiance'): dict(icon='beacon', cost=25, cooldown=30, actions=[
        effect('allies', E('resistance', 10), radius=8), effect('hostiles', E('slowness', 10), E('weakness', 10), radius=8)],
        approx='slows and weakens all hostile mobs, not only undead'),
    ('poisoner', 'Poison Needle'): dict(icon='pointed_dripstone', cost=30, cooldown=8, actions=[
        effect('look', E('poison', 6, 2), E('slowness', 4), E('nausea', 6), range=3.5)]),
    ('priest', 'Shared Recovery'): dict(icon='golden_apple', cost=15, cooldown=ONCE_PER_REST, actions=[heal('allies', 6, radius=6)],
                                       approx='heals you and every player within 6 blocks, once per 5 minutes'),
    ('pyromancer', 'Firebolt'): dict(icon='fire_charge', cost=15, cooldown=1.5, actions=[bolt(5, fire_seconds=4)]),
    ('pyromancer', 'Flame Wave'): dict(icon='blaze_powder', cost=35, cooldown=10, actions=[damage(4, radius=5, cone=90, fire_seconds=5)]),
    ('rogue', 'Nimble Recovery'): dict(icon='rabbit_foot', cooldown=ONCE_PER_REST, actions=[heal('self', percent=0.25), dash(1.0, 0.2)],
                                       approx='once per 5 minutes'),
    ('runesmith', 'Rune Blast'): dict(icon='chiseled_stone_bricks', cost=40, cooldown=15, actions=[damage(7, radius=4)],
                                      approx='a blast around you (sigils don\'t exist yet)'),
    ('scout', 'Dash'): dict(icon='sugar', cost=10, cooldown=15, actions=[effect('self', E('speed', 5, 1))]),
    ('sorcerer', 'Sorcerous Blast'): dict(icon='amethyst_cluster', cooldown=1.5, actions=[bolt(6)],
                                          approx='free to cast on a 1.5s cooldown; doesn\'t build Strain yet'),
    ('spellbinder', 'Invest Power'): dict(icon='enchanted_book', cost=20, cooldown=40, duration=20, effects=[dealt(1.3)],
                                          approx='+30% damage with anything (bound weapons don\'t exist yet)'),
    ('stormbringer', 'Lightning Bolt'): dict(icon='lightning_rod', cost=30, cooldown=10, actions=[{'type': 'pipican:lightning', 'range': 32}]),
    ('stormbringer', 'Lightning Dash'): dict(icon='trident', cost=20, cooldown=5, actions=[{'type': 'pipican:blink', 'distance': 10}]),
    ('stormbringer', 'Summon Storm'): dict(icon='lightning_rod', cost=100, cooldown=300, actions=[{'type': 'pipican:weather', 'seconds': 120}]),
    ('tenebrist', 'Shadow Form'): dict(icon='black_dye', cost=40, cooldown=40, duration=10, effects=[
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}, {'type': 'pipican:damage_taken', 'multiplier': 0.8}],
        approx='Invisibility and -20% damage taken; no passing through mobs'),
    ('tenebrist', 'Shadow Step'): dict(icon='ender_pearl', cost=25, cooldown=6, requires={'type': 'pipican:light', 'max': 8},
                                       actions=[{'type': 'pipican:blink', 'distance': 12}],
                                       approx='blink 12 blocks, usable in light level 7 or lower'),
    ('thaumaturge', 'Reroll'): dict(icon='rabbit_foot', cost=25, cooldown=20, actions=[cleanse('self')],
                                    approx='removes your harmful effects (no new random effect)'),
    ('theurge', 'Holy Word'): dict(icon='bell', cost=35, cooldown=20, actions=[effect('hostiles', stun(2), E('weakness', 5), radius=6)]),
    ('thief', 'Escape'): dict(icon='string', cost=10, cooldown=10, actions=[cleanse('self', ['slowness']), dash(0.6, 0.4)],
                              approx='removes Slowness and hops free'),
    ('traveler', 'Blink'): dict(icon='ender_pearl', cost=20, cooldown=3, actions=[{'type': 'pipican:blink', 'distance': 8}]),
    ('warrior', 'Catch Your Breath'): dict(icon='bread', cost=15, cooldown=ONCE_PER_REST, actions=[heal('self', percent=0.25)],
                                           approx='once per 5 minutes'),
    ('witch', 'Flying Broom'): dict(icon='stick', toggle=True, drain=4, cooldown=5, requires=STAFF,
                                    end_when={'type': 'pipican:not', 'condition': STAFF}, effects=[{'type': 'pipican:flight'}],
                                    approx='flies while holding a stick (or blaze rod)'),
    ('woodwose', 'Oak Hide'): dict(icon='oak_log', cost=30, cooldown=60, duration=30, effects=[attr('armor', 4)]),
}

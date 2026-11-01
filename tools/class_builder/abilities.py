"""Hand translations of the abilities described in class_specs (section 12 and "unlocks X ability").

Each entry maps (class id, ability name as written in the spec) to an ability definition. The builder
writes them to data/pipican/pipican/ability/<class>/<name>.json, gives each class the abilities that
no node unlocks, and adds "grants" to the nodes that do. An ability missing here is listed in the
build report as not in the game yet.

`approx` is a note for the report when the ability only approximates what the spec describes.
Costs are in the class's own resource; cooldowns are in seconds here (converted to ticks).
"""



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
UNDEAD = '#minecraft:undead'
UNCLEAN = '#pipican:mobs/unclean'
UNARMED = {'type': 'pipican:holding', 'items': ['minecraft:air'], 'hand': 'main'}
DARK = {'type': 'pipican:light', 'max': 8}
# "charm and fear" on a player: what clouds the mind (Nausea, Blindness, Darkness); "curses": lingering maledictions.
MIND = '#pipican:mind'
CURSES = '#pipican:curses'
BROOM = {'type': 'pipican:holding', 'items': ['minecraft:stick', 'minecraft:blaze_rod'], 'named': 'broom'}
SIGIL_GEAR = {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:item_rule', 'rule': 'sigil_weapon'},
                                                     {'type': 'pipican:item_rule', 'rule': 'sigil_armor', 'min': 1}]}


def mark(seconds, multiplier, range_=32, **kw):
    return dict({'type': 'pipican:mark', 'target': 'look', 'range': range_, 'seconds': seconds, 'multiplier': multiplier}, **kw)


def empower(multiplier=1.0, bonus=0, target='self', **kw):
    d = {'type': 'pipican:empower', 'target': target}
    if multiplier != 1:
        d['multiplier'] = multiplier
    if bonus:
        d['bonus'] = bonus
    return dict(d, **kw)


def control(mode, seconds, target='look', **kw):
    return dict({'type': 'pipican:control', 'target': target, 'mode': mode, 'seconds': seconds}, **kw)


# Hex marks its target as cursed (like Evil Eye), so "cursed mobs" bonuses and the weakness count it.
HEX_MARK = {'type': 'pipican:mark', 'target': 'look', 'range': 16, 'seconds': 10, 'multiplier': 1.0, 'glow': False}
# Reroll: a random effect for 10s, good or bad (it's chaos).
REROLL = [[effect('self', E(e, 10, amp))] for e, amp in [
    ('speed', 1), ('strength', 0), ('regeneration', 0), ('resistance', 0), ('haste', 1), ('jump_boost', 1), ('fire_resistance', 0),
    ('invisibility', 0), ('absorption', 1), ('luck', 0), ('slowness', 0), ('weakness', 0), ('nausea', 0), ('levitation', 0),
    ('glowing', 0), ('unluck', 0)]]

# Abilities used as Grimoire spells (the wizard writes one of these names on a book's first page).
GRIMOIRE_SPELLS = ['pipican:arcanist/arcane_bolt', 'pipican:pyromancer/firebolt', 'pipican:magus/staff_bolt', 'pipican:astromancer/sunbeam',
                   'pipican:astromancer/corona_burst', 'pipican:aeromancer/gust', 'pipican:hydromancer/wave', 'pipican:abjurer/ward',
                   'pipican:chronomancer/haste', 'pipican:traveler/blink', 'pipican:stormbringer/lightning_bolt', 'pipican:healer/heal']

A = {
    ('abjurer', 'Ward'): dict(icon='shield', cost=20, cooldown=15, actions=[{'type': 'pipican:absorb', 'amount': 8, 'seconds': 30}]),
    ('acrobat', 'Dash'): dict(icon='feather', cost=10, cooldown=3, actions=[dash(1.4, 0.15)]),
    ('acrobat', 'Vault'): dict(icon='rabbit_foot', cost=15, cooldown=4, actions=[dash(0.8, 0.9)]),
    ('aeromancer', 'Gust'): dict(icon='wind_charge', cost=15, cooldown=4, actions=[push(2.0, 6, cone=90)]),
    ('aeromancer', 'Flight'): dict(icon='elytra', toggle=True, drain=4, cooldown=5, effects=[{'type': 'pipican:flight'}]),
    ('arcanist', 'Arcane Bolt'): dict(icon='amethyst_shard', cost=10, cooldown=1.5, actions=[bolt(5)]),
    ('astromancer', 'Corona Burst'): dict(icon='glowstone_dust', cost=30, cooldown=15, actions=[effect('hostiles', E('blindness', 4), radius=3)]),
    ('astromancer', 'Sunbeam'): dict(icon='blaze_rod', cost=20, cooldown=4, actions=[bolt(7, 32, fire_seconds=2)]),
    ('bard', 'Dirge'): dict(icon='note_block', cost=30, cooldown=20, actions=[effect('hostiles', E('weakness', 8), E('slowness', 8), radius=10)]),
    ('beastmaster', 'Call of the Wild'): dict(icon='bone', cost=40, cooldown=60, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:wolf', 'count': 2, 'seconds': 30}]),
    ('brute', 'Ground Slam'): dict(icon='anvil', cost=30, cooldown=12, actions=[effect('hostiles', stun(2), radius=3)]),
    ('chronomancer', 'Haste'): dict(icon='sugar', cost=25, cooldown=30, actions=[effect('self', E('speed', 8, 2), E('haste', 8, 1))]),
    ('death_dealer', 'Whirlwind'): dict(icon='iron_axe', cost=40, cooldown=12, actions=[damage(6, radius=2.5, kind='melee', execute_below=20)]),
    ('destroyer', 'Cataclysm'): dict(icon='tnt', cost=60, cooldown=45, actions=[{'type': 'pipican:explode', 'power': 5}]),
    ('diplomat', 'Soothe'): dict(icon='honey_bottle', cost=15, cooldown=5, actions=[
        cleanse('look_ally', ['nausea', 'blindness', 'weakness'], range=8)]),
    ('geomancer', 'Quake'): dict(icon='cobblestone', cost=30, cooldown=15, actions=[damage(3, radius=5, kind='melee'), effect('hostiles', stun(2), radius=5)]),
    ('healer', 'Heal'): dict(icon='glistering_melon_slice', cost=15, cooldown=4, actions=[heal('look_ally', 8, range=12)]),
    ('healer', 'Mass Heal'): dict(icon='golden_apple', cost=50, cooldown=30, actions=[heal('allies', 6, radius=8)]),
    ('healer', 'Cure'): dict(icon='milk_bucket', cost=20, cooldown=10, actions=[cleanse('look_ally', range=12)]),
    ('hydromancer', 'Wave'): dict(icon='heart_of_the_sea', cost=25, cooldown=6, actions=[push(2.0, 6)]),
    ('infiltrator', 'Vanish'): dict(icon='ender_eye', cost=30, cooldown=30, actions=[effect('self', E('invisibility', 8))]),
    ('mage_knight', 'Battle Spell'): dict(icon='blaze_powder', cost=20, cooldown=6, actions=[damage(5, radius=4, cone=90), push(1.0, 4, cone=90)]),
    ('magus', 'Staff Bolt'): dict(icon='blaze_rod', cost=15, cooldown=1.5, requires=STAFF, actions=[bolt(5)]),
    ('marauder', 'Warcry'): dict(icon='goat_horn', cost=30, cooldown=20, actions=[
        effect('hostiles', E('weakness', 8), radius=8), effect('allies', E('speed', 8), radius=8)]),
    ('myrmidon', 'Shield Charge'): dict(icon='shield', cost=25, cooldown=8,
                                        requires={'type': 'pipican:holding', 'items': '#pipican:weapons/shields'},
                                        actions=[dash(1.4, 0.1), push(1.5, 3, cone=90)]),
    ('paladin', 'Lay on Hands'): dict(icon='golden_carrot', cost=15, cooldown=10, actions=[heal('look_ally', 6, range=6)]),
    ('poisoner', 'Poison Needle'): dict(icon='pointed_dripstone', cost=30, cooldown=8, actions=[
        effect('look', E('poison', 6, 2), E('slowness', 4), E('nausea', 6), range=3.5)]),
    ('pyromancer', 'Firebolt'): dict(icon='fire_charge', cost=15, cooldown=1.5, actions=[bolt(5, fire_seconds=4)]),
    ('pyromancer', 'Flame Wave'): dict(icon='blaze_powder', cost=35, cooldown=10, actions=[damage(4, radius=5, cone=90, fire_seconds=5)]),
    ('rogue', 'Nimble Recovery'): dict(icon='rabbit_foot', rest=1, cooldown=10, actions=[heal('self', percent=0.25), dash(1.0, 0.2)]),
    ('scout', 'Dash'): dict(icon='sugar', cost=10, cooldown=15, actions=[effect('self', E('speed', 5, 1))]),
    ('stormbringer', 'Lightning Bolt'): dict(icon='lightning_rod', cost=30, cooldown=10, actions=[{'type': 'pipican:lightning', 'range': 32}]),
    ('stormbringer', 'Lightning Dash'): dict(icon='trident', cost=20, cooldown=5, actions=[{'type': 'pipican:blink', 'distance': 10}]),
    ('stormbringer', 'Summon Storm'): dict(icon='lightning_rod', cost=100, cooldown=300, actions=[{'type': 'pipican:weather', 'seconds': 120}]),
    ('theurge', 'Holy Word'): dict(icon='bell', cost=35, cooldown=20, actions=[effect('hostiles', stun(2), E('weakness', 5), radius=6)]),
    ('traveler', 'Blink'): dict(icon='ender_pearl', cost=20, cooldown=3, actions=[{'type': 'pipican:blink', 'distance': 8}]),
    ('warrior', 'Catch Your Breath'): dict(icon='bread', cost=15, rest=1, cooldown=10, actions=[heal('self', percent=0.25)]),
    ('woodwose', 'Oak Hide'): dict(icon='oak_log', cost=30, cooldown=60, duration=30, effects=[attr('armor', 4)]),

    # ---- marks, next-hit buffs
    ('executioner', 'Sentence'): dict(icon='iron_axe', cost=30, cooldown=15, actions=[mark(30, 3.0, 24, next_hit=True)]),
    ('scout', 'Reveal Weakness'): dict(icon='spyglass', cost=15, cooldown=12, actions=[mark(6, 1.15, 48, shared=True)]),
    ('sharpshooter', 'Called Shot'): dict(icon='spectral_arrow', cost=25, cooldown=15, actions=[mark(30, 3.0, 64, next_hit=True, projectile_only=True)]),
    ('spellbinder', 'Sacrifice'): dict(icon='redstone_block', spend_all=True, cooldown=20,
                                       actions=[empower(1.0, bonus_per_spent=round(1 / 3, 4), seconds=15)]),

    # ---- mob control
    ('cleric', 'Turn Undead'): dict(icon='golden_apple', cost=20, cooldown=20, actions=[control('flee', 10, 'hostiles', radius=8, entities=UNDEAD)]),
    ('bard', 'Charm'): dict(icon='poppy', cost=20, cooldown=15, actions=[control('pacify', 10, range=16)]),
    ('rogue', 'Subterfuge'): dict(icon='leather', cost=10, cooldown=15, actions=[control('pacify', 10, range=16)]),
    ('enchanter', 'Compel'): dict(icon='lead', cost=40, cooldown=30, actions=[control('ally', 10, range=16)]),
    ('diplomat', 'Turncoat'): dict(icon='emerald', cost=50, cooldown=45, actions=[control('ally', 15, range=16)]),
    ('illusionist', 'Nightmare'): dict(icon='phantom_membrane', cost=30, cooldown=15, actions=[control('flee', 8, range=24)]),
    ('illusionist', 'Decoy'): dict(icon='armor_stand', cost=25, cooldown=20, actions=[{'type': 'pipican:decoy', 'seconds': 6, 'radius': 16}]),
    ('conjurer', 'Conjure Horror'): dict(icon='ravager_spawn_egg', cost=60, cooldown=60, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:ravager', 'count': 1, 'seconds': 20, 'ally': True}]),

    # ---- protection, movement, places
    ('diviner', 'Twist Fate'): dict(icon='clock', cost=30, cooldown=20, actions=[{'type': 'pipican:negate_hit', 'target': 'look_ally', 'range': 24, 'seconds': 10}]),
    ('defender', 'Intercept'): dict(icon='shield', cost=20, cooldown=12, actions=[
        {'type': 'pipican:teleport_to', 'target': 'look_ally', 'range': 24},
        {'type': 'pipican:negate_hit', 'target': 'look_ally', 'range': 24, 'seconds': 10, 'guard': True}]),
    ('ranger', 'Find the Way'): dict(icon='compass', cost=10, cooldown=10, actions=[{'type': 'pipican:locate'}]),
    ('diviner', 'Scry'): dict(icon='ender_eye', cooldown=60, actions=[{'type': 'pipican:scry', 'seconds': 5}]),

    # ---- items
    ('thief', 'Pickpocket'): dict(icon='bundle', cost=20, cooldown=20, actions=[{'type': 'pipican:steal', 'range': 4}]),

    # ---- wizard, witch, woodwose, transmuter, engineer
    ('witch', 'Lasting Bond'): dict(icon='lead', cooldown=60, actions=[{'type': 'pipican:bond', 'range': 16}]),
    # Passives written as toggles: real toggle abilities (the passive line becomes their description).
    ('oracle', 'Divine Ecstasy'): dict(icon='nether_star', toggle=True, drain=3, cooldown=5, effects=[
        attr('max_health', 6), {'type': 'pipican:immune', 'effects': ['minecraft:weakness', 'minecraft:slowness']}, dealt(1.1)],
        description='Toggle (drains 3 Ecstasy/s): +6 max health, immune to Weakness and Slowness, +10% damage.'),
    ('miracle_worker', 'Stigmata'): dict(icon='totem_of_undying', toggle=True, drain=4, cooldown=10, pulse=20,
        requires={'type': 'pipican:resource', 'min': 0.999},
        effects=[dealt(1.15), {'type': 'pipican:mob_effect', 'effect': 'minecraft:regeneration'}],
        pulse_actions=[effect('hostiles', E('slowness', 2), E('weakness', 2), radius=8)],
        description='Toggle once Miracle is full (drains 4/s): mobs within 8 blocks get Slowness and Weakness; you get +15% damage and Regeneration I.'),
    # Shapeshifter forms: one at a time (each stops the others), draining Change.
    ('shapeshifter', 'Wolf Form'): dict(icon='bone', toggle=True, drain=1.5, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[dealt(1.2), attr('movement_speed', 0.1, 'add_multiplied_base'), {'type': 'pipican:mob_effect', 'effect': 'minecraft:night_vision'}],
        description='Wolf form (drains Change): +20% damage, +10% speed, night vision.'),
    ('shapeshifter', 'Bear Form'): dict(icon='honeycomb', toggle=True, drain=1.5, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[attr('max_health', 6), attr('armor', 2), attr('knockback_resistance', 0.2), attr('movement_speed', -0.1, 'add_multiplied_base')],
        description='Bear form (drains Change): +6 max health, +2 armor, +20% knockback resistance, -10% speed.'),
    ('shapeshifter', 'Cat Form'): dict(icon='string', toggle=True, drain=1.5, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[attr('movement_speed', 0.2, 'add_multiplied_base'), {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fall', 'multiplier': 0.0}],
        description='Cat form (drains Change): +20% speed, no fall damage.'),
    ('shapeshifter', 'Fish Form'): dict(icon='cod', toggle=True, drain=1.5, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[attr('water_movement_efficiency', 0.6), {'type': 'pipican:mob_effect', 'effect': 'minecraft:water_breathing'},
                 {'type': 'pipican:mob_effect', 'effect': 'minecraft:dolphins_grace'}],
        description='Fish form (drains Change): fast swimming and water breathing.'),
    ('transmuter', 'Alter Self'): dict(icon='experience_bottle', cost=50, cooldown=60, actions=[{'type': 'pipican:optimize'}]),
    ('engineer', 'Cockpit'): dict(icon='saddle', cooldown=2, actions=[{'type': 'pipican:mount', 'range': 6}]),
    ('traveler', 'Recall'): dict(icon='recovery_compass', cost=60, cooldown=30, actions=[{'type': 'pipican:recall'}]),
    ('transmuter', 'Transmute'): dict(icon='amethyst_shard', cost=30, cooldown=5, actions=[{'type': 'pipican:transmute', 'pairs': [
        {'from': 'minecraft:' + a, 'to': 'minecraft:' + b} for a, b in [
            ('cobblestone', 'stone'), ('stone', 'smooth_stone'), ('sand', 'glass'), ('gravel', 'flint'), ('dirt', 'grass_block'),
            ('cobbled_deepslate', 'deepslate'), ('netherrack', 'nether_bricks'), ('clay_ball', 'brick'), ('raw_iron', 'iron_ingot'),
            ('raw_copper', 'copper_ingot'), ('raw_gold', 'gold_ingot'), ('rotten_flesh', 'leather'), ('oak_log', 'spruce_log'),
            ('spruce_log', 'birch_log'), ('birch_log', 'jungle_log'), ('jungle_log', 'acacia_log'), ('acacia_log', 'dark_oak_log'),
            ('dark_oak_log', 'oak_log')]]}]),
    ('wizard', 'Grimoire Casting'): dict(icon='writable_book', cost=10, cooldown=2, actions=[{'type': 'pipican:cast', 'spells': GRIMOIRE_SPELLS}]),
    ('engineer', 'Build Eidolon'): dict(icon='iron_block', cooldown=5, actions=[{'type': 'pipican:eidolon', 'item': 'minecraft:iron_block', 'count': 4, 'rebuild_ticks': 8000}]),

    # ---- abilities built to the spec text (section 12)
    ('abjurer', 'Sanctuary'): dict(icon='end_crystal', cost=40, cooldown=40, duration=5, anchored=True, pulse=2, pulse_actions=[
        push(0.6, 3, from_origin=True)],
        description='Mobs can\'t enter a 3-block bubble around where you stood for 5s.'),
    ('apocalyptist', 'Hasten the Apocalypse'): dict(icon='wither_rose', cost=40, cooldown=25, duration=8, anchored=True, pulse=20, actions=[
        damage(6, radius=8), effect('hostiles', stun(2), radius=8)],
        pulse_actions=[effect('hostiles', E('slowness', 1.5, 2), radius=8)],
        description='A wave of green light: mobs within 8 blocks take 6 damage and fall prone for 2s, and the ground there slows them for 8s.'),
    ('artificer', 'Mechanical Servants'): dict(icon='copper_block', cost=40, cooldown=90, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:copper_golem', 'count': 2, 'seconds': 30, 'ally': True}],
        description='Two small copper golems fight for you for 30s.'),
    ('assassin', 'Disguise'): dict(icon='leather_helmet', cost=25, cooldown=30, duration=10, effects=[
        {'type': 'pipican:ignored_by', 'mobs': '#pipican:mobs/monsters', 'unless_attacked': True}],
        description='Mobs ignore you for 10s (one you attack may fight back).'),
    ('beastmaster', 'Beast Within'): dict(icon='rabbit_hide', cost=20, cooldown=40, duration=20, effects=[
        {'type': 'pipican:conditional', 'when': UNARMED, 'effects': [attr('attack_damage', 2)]},
        attr('movement_speed', 0.15, 'add_multiplied_base')],
        description='Claws for 20s: +2 unarmed damage and +15% speed.'),
    ('cavalier', 'Charge'): dict(icon='saddle', cost=25, cooldown=10, actions=[
        {'type': 'pipican:charge', 'strength': 1.8, 'up': 0.1, 'damage': 8, 'push': 1.5, 'seconds': 1.2}],
        description='Charge forward: every mob you run into in the next second takes 8 damage and is thrown back.'),
    ('chronomancer', 'Time Stop'): dict(icon='clock', cost=60, cooldown=60, actions=[
        {'type': 'pipican:freeze', 'target': 'hostiles', 'radius': 6, 'seconds': 3}],
        description='Mobs within 6 blocks are frozen in time for 3s.'),
    ('dervish', 'Whirl'): dict(icon='iron_sword', cost=25, cooldown=6, actions=[
        {'type': 'pipican:weapon_strike', 'target': 'hostiles', 'radius': 3, 'hands': 'both'}],
        description='Spin, hitting every mob within 3 blocks with both weapons.'),
    ('enchanter', 'Countercharm'): dict(icon='enchanted_book', cost=20, cooldown=15, actions=[
        {'type': 'pipican:cleanse', 'target': 'allies', 'radius': 8, 'effects': MIND}],
        description='You and players within 8 blocks are freed of charm and fear (Nausea, Blindness, Darkness).'),
    ('exorcist', 'Exorcise'): dict(icon='totem_of_undying', cost=25, cooldown=10, actions=[
        {'type': 'pipican:cleanse', 'target': 'look_ally', 'range': 8, 'effects': MIND},
        {'type': 'pipican:cleanse', 'target': 'look_ally', 'range': 8, 'effects': CURSES},
        effect('look', stun(2), range=8, entities=UNCLEAN, required=False)],
        description='Cleanse charm, fear and curses from the player you look at (or yourself), or stun the unclean mob you look at for 2s.'),
    ('gladiator', 'Coup de Grace'): dict(icon='netherite_sword', cost=30, cooldown=10, actions=[
        bolt(10, 4, kind='melee', state={'harmful_effect': True})],
        description='A heavy finishing blow (10 damage) on the mob you look at, if it suffers a harmful effect.'),
    ('gunslinger', 'Deadeye'): dict(icon='spyglass', cost=30, cooldown=20, duration=2, actions=[
        effect('hostiles', E('slowness', 2, 3), E('mining_fatigue', 2, 2), radius=32)],
        effects=[{'type': 'pipican:bonus', 'kind': 'ranged_crit', 'amount': 1}],
        description='Time slows around you for 2s (mobs within 32 blocks crawl) and every shot you fire crits.'),
    ('hexer', 'Hex'): dict(icon='fermented_spider_eye', cost=20, cooldown=8, actions=[{'type': 'pipican:choice', 'options': [
        {'name': 'Desire', 'actions': [HEX_MARK, control('pacify', 10, range=16)]},
        {'name': 'Lethargy', 'actions': [HEX_MARK, effect('look', E('slowness', 10, 1), range=16)]},
        {'name': 'Pain', 'actions': [HEX_MARK, {'type': 'pipican:dot', 'target': 'look', 'range': 16, 'damage': 2, 'seconds': 10}]}]}],
        description='Curse the mob you look at for 10s with the curse you chose (sneak + use to choose): Desire, Lethargy or Pain.'),
    ('hydromancer', 'Watery Form'): dict(icon='water_bucket', cost=40, cooldown=40, duration=10, effects=[
        dealt(0.5, '#pipican:weapons/melee'), dealt(0.5, '#pipican:weapons/ranged'), attr('scale', -0.5, 'add_multiplied_base'),
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:dolphins_grace'},
        {'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'underwater'},
         'effects': [{'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}]}],
        description='Become water for 10s: half weapon damage, small enough to slip through 1-block gaps, fast swimming, invisible underwater.'),
    ('magician', 'Spell Recovery'): dict(icon='lapis_lazuli', rest=1, cooldown=10, actions=[
        {'type': 'pipican:resource', 'percent': 1.0}, heal('self', 4)],
        description='Once per rest: refill your Mana and heal 4.'),
    ('paladin', 'Holy Radiance'): dict(icon='beacon', cost=25, cooldown=30, actions=[
        effect('allies', E('resistance', 10), radius=8), effect('hostiles', E('slowness', 10), E('weakness', 10), radius=8, entities=UNDEAD)],
        description='Light aura: you and players within 8 blocks resist (Resistance I), undead there are slowed and weakened for 10s.'),
    ('priest', 'Shared Recovery'): dict(icon='golden_apple', cost=15, rest=1, cooldown=10, actions=[
        heal('self', 6), heal('nearest_player', 6, radius=6)],
        description='Once per rest: heal yourself and the nearest player within 6 blocks for 6.'),
    ('runesmith', 'Rune Blast'): dict(icon='chiseled_stone_bricks', cost=40, cooldown=15, requires=SIGIL_GEAR, actions=[
        damage(4, radius=4, scaling=[{'source': 'sigils', 'per': 1.5}])],
        description='Detonate your sigils: mobs within 4 blocks take 4 damage +1.5 per sigil you carry (sigil weapon in hand, trimmed armor).'),
    ('sorcerer', 'Sorcerous Blast'): dict(icon='amethyst_cluster', cost=1, cooldown=1.5, actions=[bolt(6)],
        description='Spend 1 Strain: a magic bolt for 6 damage.'),
    ('spellbinder', 'Invest Power'): dict(icon='enchanted_book', cost=20, cooldown=40, duration=20, effects=[
        {'type': 'pipican:conditional', 'when': {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}, 'effects': [dealt(1.3)]}],
        description='+30% damage with your bound weapon for 20s.'),
    ('tenebrist', 'Shadow Form'): dict(icon='black_dye', cost=40, cooldown=40, duration=10, effects=[
        {'type': 'pipican:dodge', 'chance': 0.3}, {'type': 'pipican:bonus', 'kind': 'unpushable', 'amount': 1},
        {'type': 'pipican:conditional', 'when': DARK, 'effects': [{'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}]}],
        description='Become shadowy for 10s: attacks miss you 30% of the time, you pass through mobs, and you are invisible in darkness.'),
    ('tenebrist', 'Shadow Step'): dict(icon='ender_pearl', cost=25, cooldown=6, requires=DARK,
        actions=[{'type': 'pipican:blink', 'distance': 16, 'max_light': 7}],
        description='From a shadow, step to the farthest shadow (light 7 or lower) up to 16 blocks where you look.'),
    ('thaumaturge', 'Reroll'): dict(icon='rabbit_foot', cost=25, cooldown=20, actions=[
        {'type': 'pipican:cleanse', 'target': 'self', 'last': True}, {'type': 'pipican:random', 'choices': REROLL}],
        description='Cancel the last harmful effect on you and roll a new random effect (good or bad) for 10s.'),
    ('thief', 'Escape'): dict(icon='string', cost=10, cooldown=10, actions=[{'type': 'pipican:break_free', 'seconds': 3}],
        description='Break free of Slowness, cobwebs and whatever holds you (guardian beams, leads, mobs carrying you); Slowness can\'t touch you for 3s.'),
    ('witch', 'Flying Broom'): dict(icon='stick', toggle=True, drain=4, cooldown=5, requires=BROOM,
        end_when={'type': 'pipican:not', 'condition': BROOM}, effects=[{'type': 'pipican:flight'}],
        description='Fly while you hold a broom - a stick or blaze rod renamed "Broom" at an anvil (drains Witchery).'),
    ('assassin', 'Mark Target'): dict(icon='crossbow', cost=15, cooldown=10, actions=[mark(60, 1.3, state={'not_targeting_you': True})],
        description='Mark the mob you look at that hasn\'t noticed you: +30% damage to it for 60s.'),
    ('avenger', 'Declare Vengeance'): dict(icon='redstone', cost=20, cooldown=10, actions=[mark(60, 1.0, 64)],
        description='Mark any mob you can see (up to 64 blocks) as your marked target for 60s.'),
    ('cleric', 'Holy Strike'): dict(icon='golden_sword', cost=15, cooldown=8, actions=[empower(1.0, 0, seconds=10, against=UNDEAD, bonus_roll=6)],
        description='Your next hit on an undead mob within 10s deals 1-6 extra damage.'),
    ('weapon_master', 'Masterstroke'): dict(icon='netherite_sword', cost=30, cooldown=20, actions=[
        empower(1.5, seconds=10, requires={'type': 'pipican:holding_favored'})],
        description='Your next hit with your favored weapon within 10s is a guaranteed critical (+50%).'),
    ('conqueror', 'Attack Command'): dict(icon='goat_horn', cost=20, cooldown=15, actions=[empower(1.5, target='look_ally', range=24, seconds=10)],
        description='The ally you look at gets a free empowered hit: +50% on their next hit within 10s.'),
    ('artificer', 'Store Spell'): dict(icon='brewing_stand', cost=15, cooldown=10, actions=[{'type': 'pipican:store_spell', 'charges': 8}],
        description='Pour the potion in your off hand into the weapon or armor in your main hand: it casts the potion 8 times (weapons on hit, armor when you\'re hit).'),
    ('inquisitor', 'Dreadful Threat'): dict(icon='iron_bars', cost=25, cooldown=15, actions=[control('flee', 8, range=24, state={'marked': True})],
        description='Frighten the scrutinised mob you look at: it flees for 8s.'),
    ('technomancer', 'Animate Object'): dict(icon='anvil', cost=50, cooldown=60, actions=[{'type': 'pipican:animate', 'seconds': 60, 'range': 6}],
        description='Turn the placed block you look at (an anvil, a cauldron...) into a friendly construct for 60s; harder blocks make tougher constructs.'),
    ('necromancer', 'Raise Dead'): dict(icon='zombie_head', cost=40, cooldown=45, duration=30, effects=[
        {'type': 'pipican:on', 'event': 'kill', 'actions': [
            {'type': 'pipican:summon', 'entity': 'minecraft:zombie', 'count': 1, 'seconds': 60, 'ally': True, 'unless_victims': '#minecraft:skeletons'},
            {'type': 'pipican:summon', 'entity': 'minecraft:skeleton', 'count': 1, 'seconds': 60, 'ally': True, 'victims': '#minecraft:skeletons'},
            {'type': 'pipican:stop', 'abilities': ['pipican:necromancer/raise_dead']}]}],
        description='The next mob you kill within 30s rises as your servant for 60s - a skeleton if it was one, otherwise a zombie.'),
    ('conqueror', 'Direct the Troops'): dict(icon='white_banner', cost=20, cooldown=15, actions=[
        {'type': 'pipican:dash', 'target': 'players', 'radius': 12, 'strength': 1.2, 'up': 0.2, 'toward_look': True}],
        description='Players within 12 blocks dash toward the spot you look at.'),
    ('druid', 'Tree Walk'): dict(icon='oak_log', cost=10, cooldown=5, actions=[
        {'type': 'pipican:blink', 'distance': 24, 'require_blocks': '#minecraft:logs', 'start_near': '#minecraft:logs'}],
        description='Step into a log you touch and come out of the log you look at, up to 24 blocks away.'),
    ('geomancer', 'Earth Glide'): dict(icon='stone', cost=40, cooldown=15, duration=3, effects=[
        {'type': 'pipican:bonus', 'kind': 'phase_stone', 'amount': 1}], end_actions=[{'type': 'pipican:surface'}],
        description='Walk through stone, dirt and ores for 3s (sneak to sink); you come up for air when it ends.'),
    ('oracle', 'Commune'): dict(icon='amethyst_cluster', cost=30, cooldown=60, actions=[{'type': 'pipican:locate', 'any_of': [
        '#minecraft:village', '#minecraft:ruined_portal', '#minecraft:shipwreck', '#minecraft:on_woodland_explorer_maps',
        '#minecraft:on_ocean_explorer_maps', '#minecraft:on_trial_chambers_maps']}],
        description='Ask the gods: points you toward the nearest structure (village, ruined portal, shipwreck, mansion, monument or trial chamber).'),
    ('templar', 'Consecrate Ground'): dict(icon='beacon', cost=50, cooldown=90, actions=[{'type': 'pipican:temple', 'seconds': 60}],
        description='Raise a temporary temple where you stand for 60s: all your temple bonuses work around it.'),
    ('artificer', 'Assemble'): dict(icon='iron_sword', cost=20, cooldown=30, actions=[
        {'type': 'pipican:give', 'item': 'minecraft:iron_sword', 'seconds': 0,
         'sneak_items': ['minecraft:iron_helmet', 'minecraft:iron_chestplate', 'minecraft:iron_leggings', 'minecraft:iron_boots']}],
        description='Spend Spare Parts on a temporary iron sword (sneak: the iron armor piece you\'re missing, worn at once); it vanishes when you sleep.'),
    ('warlock', 'Return to Sender'): dict(icon='splash_potion', cost=10, cooldown=10, actions=[{'type': 'pipican:throw_stolen'}],
        description='Throw the last effect your Steal Spell took from an attacker as a splash potion.'),
    ('witch', 'Witch Fire'): dict(icon='fire_charge', toggle=True, drain=15, cooldown=10, pulse=1, effects=[
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}],
        pulse_actions=[{'type': 'pipican:steer', 'speed': 0.6, 'damage': 3}], end_actions=[{'type': 'pipican:steer', 'stop': True}],
        description='Become invisible and steer a green fireball with your gaze; it burns the mobs it touches (drains 15 Witchery/s).'),
    ('shapeshifter', 'Bird Form'): dict(icon='feather', toggle=True, drain=2, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[{'type': 'pipican:bonus', 'kind': 'glide', 'amount': 1}, {'type': 'pipican:mob_effect', 'effect': 'minecraft:slow_falling'},
                 attr('max_health', -0.5, 'add_multiplied_base')],
        description='Bird form (drains Change): glide (hold jump while falling) and slow fall, -50% max health.'),
    ('woodwose', 'Treeform'): dict(icon='oak_sapling', toggle=True, cooldown=10, pulse=20, effects=[
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}, {'type': 'pipican:mob_effect', 'effect': 'minecraft:slowness', 'amplifier': 9},
        attr('jump_strength', -1, 'add_multiplied_total'), {'type': 'pipican:ignored_by', 'mobs': '#pipican:mobs/monsters'}],
        pulse_actions=[heal('self', percent=0.1)],
        description='Become a tree: fully hidden (invisible, mobs ignore you) and rooted, healing 10% of your health every second until whole.'),
    ('spellbinder', 'Recall Weapon'): dict(icon='trident', cost=10, cooldown=3, actions=[
        {'type': 'pipican:recall_item', 'items': '#pipican:weapons/melee', 'radius': 64, 'bound': True}],
        description='Your bound weapon (hotbar slot 1) flies back into your hand, wherever it is within 64 blocks.'),
    ('engineer', 'Overdrive'): dict(icon='redstone', cost=40, cooldown=30, actions=[
        {'type': 'pipican:attribute_boost', 'target': 'companions', 'radius': 32, 'entities': 'pipican:eidolon', 'seconds': 10, 'attributes': [
            {'attribute': 'minecraft:movement_speed', 'amount': 0.5}, {'attribute': 'minecraft:attack_damage', 'amount': 0.5}]}],
        description='Your eidolon gets +50% speed and damage for 10s.'),
}

# Abilities a class has even though section 12 doesn't list them (the spec describes them elsewhere).
EXTRA = {
    'engineer': ['Build Eidolon'],
    'oracle': ['Divine Ecstasy'],
    'miracle_worker': ['Stigmata'],
    'shapeshifter': ['Wolf Form', 'Bear Form', 'Cat Form', 'Fish Form', 'Bird Form'],
}

# Spec abilities that are really passive rules, handled by NODE_EFFECTS or passives instead.
NOT_ABILITIES = {('wizard', 'Spell Mastery'): 'the Spell Mastery keystone node gives spell points'}

# Hand-written effects for node texts the translator can't read: (class, node id) -> (effects, grants).
NODE_EFFECTS = {
    ('wizard', 'spell_mastery'): ([{'type': 'pipican:spell_points', 'amount': 6},
                                   {'type': 'pipican:resource', 'target': 'regen', 'amount': -0.1, 'operation': 'add_multiplied_base'}], []),
    ('witch', 'lasting_bond'): ([], ['Lasting Bond']),
    ('shapeshifter', 'bird_form'): ([], ['Bird Form']),
}

# Hand-written effects for passive lines: (class, start of the line) -> effects.
PASSIVE_EFFECTS = {
    ('transmuter', 'Optimization:'): [{'type': 'pipican:optimization', 'amount': 3,
                                       'scaling': [{'source': 'special_stat', 'per': 0.05}]}],
    ('engineer', 'Eidolon:'): [],       # the eidolon itself: the Build Eidolon ability (EXTRA)
    ('engineer', 'Spare Parts:'): [],   # built into the eidolon: iron ingots heal it 10
    ('oracle', 'Divine Ecstasy (toggle'): [],   # the Divine Ecstasy ability (EXTRA)
    ('miracle_worker', 'Stigmata:'): [],        # the Stigmata ability (EXTRA)
    ('shapeshifter', 'Superior Transformation:'): [],   # the form abilities (EXTRA)
}

# Hand-written weakness effects: class -> effects (replace the translated ones).
WEAKNESS_EFFECTS = {
    'transmuter': [{'type': 'pipican:optimization', 'extra_loss': 3}],  # death reset is built into Optimization
}

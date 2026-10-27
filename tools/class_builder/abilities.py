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


# Abilities used as Grimoire spells (the wizard writes one of these names on a book's first page).
GRIMOIRE_SPELLS = ['pipican:arcanist/arcane_bolt', 'pipican:pyromancer/firebolt', 'pipican:magus/staff_bolt', 'pipican:astromancer/sunbeam',
                   'pipican:astromancer/corona_burst', 'pipican:aeromancer/gust', 'pipican:hydromancer/wave', 'pipican:abjurer/ward',
                   'pipican:chronomancer/haste', 'pipican:traveler/blink', 'pipican:stormbringer/lightning_bolt', 'pipican:healer/heal']

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
    ('magician', 'Spell Recovery'): dict(icon='lapis_lazuli', rest=1, cooldown=10, actions=[
        {'type': 'pipican:resource', 'percent': 0.5}, heal('self', 4)], approx='refills half the Mana'),
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
    ('priest', 'Shared Recovery'): dict(icon='golden_apple', cost=15, rest=1, cooldown=10, actions=[heal('allies', 6, radius=6)],
                                       approx='heals you and every player within 6 blocks'),
    ('pyromancer', 'Firebolt'): dict(icon='fire_charge', cost=15, cooldown=1.5, actions=[bolt(5, fire_seconds=4)]),
    ('pyromancer', 'Flame Wave'): dict(icon='blaze_powder', cost=35, cooldown=10, actions=[damage(4, radius=5, cone=90, fire_seconds=5)]),
    ('rogue', 'Nimble Recovery'): dict(icon='rabbit_foot', rest=1, cooldown=10, actions=[heal('self', percent=0.25), dash(1.0, 0.2)]),
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
    ('warrior', 'Catch Your Breath'): dict(icon='bread', cost=15, rest=1, cooldown=10, actions=[heal('self', percent=0.25)]),
    ('witch', 'Flying Broom'): dict(icon='stick', toggle=True, drain=4, cooldown=5, requires=STAFF,
                                    end_when={'type': 'pipican:not', 'condition': STAFF}, effects=[{'type': 'pipican:flight'}],
                                    approx='flies while holding a stick (or blaze rod)'),
    ('woodwose', 'Oak Hide'): dict(icon='oak_log', cost=30, cooldown=60, duration=30, effects=[attr('armor', 4)]),

    # ---- marks, next-hit buffs
    ('assassin', 'Mark Target'): dict(icon='crossbow', cost=15, cooldown=10, actions=[mark(60, 1.3)],
                                      approx='works on any mob in sight, not only ones you are hidden from'),
    ('avenger', 'Declare Vengeance'): dict(icon='redstone', cost=20, cooldown=10, actions=[mark(60, 1.25, 64)],
                                           approx='+25% damage to the marked mob for 60s'),
    ('executioner', 'Sentence'): dict(icon='iron_axe', cost=30, cooldown=15, actions=[mark(30, 3.0, 24, next_hit=True)]),
    ('scout', 'Reveal Weakness'): dict(icon='spyglass', cost=15, cooldown=12, actions=[mark(6, 1.15, 48, shared=True)]),
    ('sharpshooter', 'Called Shot'): dict(icon='spectral_arrow', cost=25, cooldown=15, actions=[mark(30, 3.0, 64, next_hit=True, projectile_only=True)]),
    ('cleric', 'Holy Strike'): dict(icon='golden_sword', cost=15, cooldown=8, actions=[empower(1.25, 3.5, seconds=10, against=UNDEAD)],
                                    approx='your next hit on an undead mob within 10s: +25% and +3.5 damage'),
    ('weapon_master', 'Masterstroke'): dict(icon='netherite_sword', cost=30, cooldown=20, actions=[empower(2.0, seconds=10)],
                                            approx='your next hit within 10s deals double damage, with any weapon'),
    ('spellbinder', 'Sacrifice'): dict(icon='redstone_block', spend_all=True, cooldown=20,
                                       actions=[empower(1.0, bonus_per_spent=round(1 / 3, 4), seconds=15)]),
    ('conqueror', 'Attack Command'): dict(icon='goat_horn', cost=20, cooldown=15, actions=[empower(1.5, target='look_ally', range=24, seconds=10)],
                                          approx='the ally you look at (or you) deals +50% on their next hit within 10s'),
    ('artificer', 'Store Spell'): dict(icon='brewing_stand', cost=15, cooldown=10, actions=[{'type': 'pipican:imbue', 'hits': 5, 'seconds': 60}],
                                       approx='uses up the potion in your off hand: your next 5 hits within 60s apply it'),

    # ---- mob control
    ('cleric', 'Turn Undead'): dict(icon='golden_apple', cost=20, cooldown=20, actions=[control('flee', 10, 'hostiles', radius=8, entities=UNDEAD)]),
    ('bard', 'Charm'): dict(icon='poppy', cost=20, cooldown=15, actions=[control('pacify', 10, range=16)]),
    ('rogue', 'Subterfuge'): dict(icon='leather', cost=10, cooldown=15, actions=[control('pacify', 10, range=16)]),
    ('enchanter', 'Compel'): dict(icon='lead', cost=40, cooldown=30, actions=[control('ally', 10, range=16)]),
    ('diplomat', 'Turncoat'): dict(icon='emerald', cost=50, cooldown=45, actions=[control('ally', 15, range=16)]),
    ('illusionist', 'Nightmare'): dict(icon='phantom_membrane', cost=30, cooldown=15, actions=[control('flee', 8, range=24)]),
    ('inquisitor', 'Dreadful Threat'): dict(icon='iron_bars', cost=25, cooldown=15, actions=[control('flee', 8, range=24)],
                                            approx='any mob you look at (scrutiny isn\'t tracked yet)'),
    ('illusionist', 'Decoy'): dict(icon='armor_stand', cost=25, cooldown=20, actions=[{'type': 'pipican:decoy', 'seconds': 6, 'radius': 16}]),
    ('conjurer', 'Conjure Horror'): dict(icon='ravager_spawn_egg', cost=60, cooldown=60, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:ravager', 'count': 1, 'seconds': 20, 'ally': True}]),
    ('technomancer', 'Animate Object'): dict(icon='anvil', cost=50, cooldown=60, actions=[
        {'type': 'pipican:summon', 'entity': 'minecraft:iron_golem', 'count': 1, 'seconds': 60, 'ally': True}],
        approx='an allied iron golem for 60s (you don\'t need to pick a block)'),
    ('necromancer', 'Raise Dead'): dict(icon='zombie_head', cost=40, cooldown=45, duration=30, effects=[
        {'type': 'pipican:on', 'event': 'kill', 'actions': [{'type': 'pipican:summon', 'entity': 'minecraft:zombie', 'count': 1, 'seconds': 30, 'ally': True}]}],
        approx='for 30s, every mob you kill rises as an allied zombie for 30s'),

    # ---- protection, movement, places
    ('diviner', 'Twist Fate'): dict(icon='clock', cost=30, cooldown=20, actions=[{'type': 'pipican:negate_hit', 'target': 'look_ally', 'range': 24, 'seconds': 10}]),
    ('defender', 'Intercept'): dict(icon='shield', cost=20, cooldown=12, actions=[
        {'type': 'pipican:teleport_to', 'target': 'look_ally', 'range': 24},
        {'type': 'pipican:negate_hit', 'target': 'look_ally', 'range': 24, 'seconds': 10, 'guard': True}]),
    ('conqueror', 'Direct the Troops'): dict(icon='white_banner', cost=20, cooldown=15, actions=[
        {'type': 'pipican:dash', 'target': 'allies', 'radius': 12, 'strength': 1.2, 'up': 0.2}],
        approx='you and nearby players charge the way you look'),
    ('druid', 'Tree Walk'): dict(icon='oak_log', cost=10, cooldown=5, actions=[{'type': 'pipican:blink', 'distance': 24, 'require_blocks': '#minecraft:logs'}],
                                 approx='steps out beside the log you look at, up to 24 blocks away'),
    ('geomancer', 'Earth Glide'): dict(icon='stone', cost=40, cooldown=15, actions=[{'type': 'pipican:blink', 'distance': 8, 'through_walls': True}],
                                       approx='moves you up to 8 blocks through stone at once'),
    ('traveler', 'Recall'): dict(icon='recovery_compass', cost=60, cooldown=30, actions=[{'type': 'pipican:recall'}],
                                 approx='sneak + use to mark the spot (free), use to return'),
    ('ranger', 'Find the Way'): dict(icon='compass', cost=10, cooldown=10, actions=[{'type': 'pipican:locate'}]),
    ('oracle', 'Commune'): dict(icon='amethyst_cluster', cost=30, cooldown=60, actions=[{'type': 'pipican:locate', 'structures': '#minecraft:village'}],
                                approx='points you toward the nearest village'),
    ('diviner', 'Scry'): dict(icon='ender_eye', cooldown=60, actions=[{'type': 'pipican:scry', 'seconds': 5}]),
    ('templar', 'Consecrate Ground'): dict(icon='beacon', cost=50, cooldown=90, duration=60, anchored=True, pulse=20, pulse_actions=[
        effect('allies', E('regeneration', 2.5), radius=6), effect('hostiles', E('slowness', 2), radius=6),
        damage(1, radius=6, entities=UNDEAD)],
        approx='a 6-block circle where you stand for 60s: allies regenerate, mobs are slowed, undead are burned'),

    # ---- items
    ('artificer', 'Assemble'): dict(icon='iron_sword', cost=20, cooldown=30, actions=[
        {'type': 'pipican:give', 'item': 'minecraft:iron_sword', 'sneak_item': 'minecraft:iron_chestplate', 'seconds': 600}],
        approx='an iron sword (iron chestplate while sneaking) that vanishes after 10 minutes or when you sleep'),
    ('thief', 'Pickpocket'): dict(icon='bundle', cost=20, cooldown=20, actions=[{'type': 'pipican:steal', 'range': 4}]),
    ('transmuter', 'Transmute'): dict(icon='amethyst_shard', cost=30, cooldown=5, actions=[{'type': 'pipican:transmute', 'pairs': [
        {'from': 'minecraft:' + a, 'to': 'minecraft:' + b} for a, b in [
            ('cobblestone', 'stone'), ('stone', 'smooth_stone'), ('sand', 'glass'), ('gravel', 'flint'), ('dirt', 'grass_block'),
            ('cobbled_deepslate', 'deepslate'), ('netherrack', 'nether_bricks'), ('clay_ball', 'brick'), ('raw_iron', 'iron_ingot'),
            ('raw_copper', 'copper_ingot'), ('raw_gold', 'gold_ingot'), ('rotten_flesh', 'leather'), ('oak_log', 'spruce_log'),
            ('spruce_log', 'birch_log'), ('birch_log', 'jungle_log'), ('jungle_log', 'acacia_log'), ('acacia_log', 'dark_oak_log'),
            ('dark_oak_log', 'oak_log')]]}],
        approx='turns the held stack into a related material (cobblestone to stone, sand to glass, raw ores to ingots...)'),
    ('warlock', 'Return to Sender'): dict(icon='splash_potion', cost=10, cooldown=10, actions=[{'type': 'pipican:transfer_effects', 'target': 'look', 'range': 16}],
                                          approx='moves your harmful effects onto the mob you look at'),

    # ---- wizard, witch, woodwose, transmuter, engineer
    ('wizard', 'Grimoire Casting'): dict(icon='writable_book', cost=10, cooldown=2, actions=[{'type': 'pipican:cast', 'spells': GRIMOIRE_SPELLS}],
                                         approx='write a spell name (e.g. "Firebolt", "Blink", "Haste") on the first page of a book and quill'),
    ('witch', 'Witch Fire'): dict(icon='fire_charge', toggle=True, drain=15, cooldown=10, pulse=20, effects=[
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}],
        pulse_actions=[damage(3, 'look', range=16, required=False, fire_seconds=3)],
        approx='invisible while on; every second burns the mob you look at (no steering)'),
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
    ('shapeshifter', 'Bird Form'): dict(icon='feather', toggle=True, drain=2, cooldown=3, actions=[{'type': 'pipican:stop', 'abilities': 'FORMS'}],
        effects=[{'type': 'pipican:mob_effect', 'effect': 'minecraft:slow_falling'}, attr('max_health', -0.5, 'add_multiplied_base')],
        description='Bird form (drains Change): slow fall, -50% max health.', approx='slow fall instead of gliding'),
    ('woodwose', 'Treeform'): dict(icon='oak_sapling', toggle=True, cooldown=10, effects=[
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}, {'type': 'pipican:mob_effect', 'effect': 'minecraft:slowness', 'amplifier': 9},
        {'type': 'pipican:mob_effect', 'effect': 'minecraft:regeneration', 'amplifier': 1}],
        approx='hidden and rooted while on, regenerating fast'),
    ('transmuter', 'Alter Self'): dict(icon='experience_bottle', cost=50, cooldown=60, actions=[{'type': 'pipican:optimize'}]),
    ('engineer', 'Build Eidolon'): dict(icon='iron_block', cooldown=5, actions=[{'type': 'pipican:eidolon', 'item': 'minecraft:iron_block', 'count': 4, 'rebuild_ticks': 8000}],
                                        approx='uses 4 iron blocks; if your eidolon exists it comes to you instead'),
    ('engineer', 'Cockpit'): dict(icon='saddle', cooldown=2, actions=[{'type': 'pipican:mount', 'range': 6}]),
    ('spellbinder', 'Recall Weapon'): dict(icon='trident', cost=10, cooldown=3, actions=[
        {'type': 'pipican:recall_item', 'items': '#pipican:weapons/melee', 'radius': 64}],
        approx='calls back the nearest weapon you dropped or threw (weapons aren\'t bound yet)'),
    ('engineer', 'Overdrive'): dict(icon='redstone', cost=40, cooldown=30, actions=[effect('companions', E('speed', 10, 1), E('strength', 10, 1), radius=32)],
                                    approx='Speed II and Strength II on your eidolon (and other companions) for 10s'),
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

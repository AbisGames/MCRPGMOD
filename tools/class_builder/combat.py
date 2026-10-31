"""Phase-B phrases: bonuses against kinds of mobs, victim states, crits, dodge, immunities and on-hit reactions.

Each function takes a lower-cased clause (already cleaned) and the translator context, and returns a list
of effects or None. effects.py calls them before its generic rules.
"""
import re

NUM = r'([+-]?\d+(?:\.\d+)?)'

# Mob groups: spec words -> entity ids / tags. Several groups in one phrase become one generated tag.
MOB_GROUPS = {
    'undead': ['#minecraft:undead'], 'undead mobs': ['#minecraft:undead'],
    'water, snow and ice mobs': ['minecraft:drowned', 'minecraft:guardian', 'minecraft:elder_guardian', 'minecraft:stray', 'minecraft:snow_golem',
                                 'minecraft:polar_bear', 'minecraft:squid', 'minecraft:glow_squid', 'minecraft:axolotl', 'minecraft:dolphin'],
    'nether mobs': ['#pipican:mobs/nether'], 'unclean mobs': ['#pipican:mobs/unclean'], 'unclean': ['#pipican:mobs/unclean'],
    'animals': ['#pipican:mobs/animals'], 'beasts': ['#pipican:mobs/animals'], 'animals and beasts': ['#pipican:mobs/animals'],
    'illagers': ['#minecraft:illager'], 'raiders': ['#minecraft:raiders'], 'arthropods': ['#minecraft:arthropod'],
    'spiders': ['minecraft:spider', 'minecraft:cave_spider'], 'aquatic mobs': ['#minecraft:aquatic'], 'water mobs': ['#minecraft:aquatic'],
    'skeletons': ['#minecraft:skeletons'], 'zombies': ['#minecraft:zombies'], 'golems': ['minecraft:iron_golem', 'minecraft:snow_golem'],
    'endermen': ['minecraft:enderman'], 'end mobs': ['#pipican:mobs/end'], 'vexes': ['minecraft:vex'], 'phantoms': ['minecraft:phantom'],
    'the wither': ['minecraft:wither'], 'wither skeletons': ['minecraft:wither_skeleton'], 'wardens': ['minecraft:warden'],
    'piglins': ['minecraft:piglin', 'minecraft:piglin_brute'], 'guardians': ['minecraft:guardian', 'minecraft:elder_guardian'],
    'drowned': ['minecraft:drowned'], 'witches': ['minecraft:witch'], 'slimes': ['minecraft:slime', 'minecraft:magma_cube'],
    'blazes': ['minecraft:blaze'], 'creepers': ['minecraft:creeper'], 'ravagers': ['minecraft:ravager'], 'ghasts': ['minecraft:ghast'],
    'mobs that burn in daylight': ['#pipican:mobs/daylight'], 'beast-like mobs': ['#pipican:mobs/beasts'],
    'beast-like mobs (wolves, hoglins, ravagers)': ['#pipican:mobs/beasts'],
    'neutral mobs': ['#pipican:mobs/neutral'],
    'mindless mobs': ['#pipican:mobs/mindless'], 'armored mobs': ['#pipican:mobs/armored'],
    'mobs with natural armor': ['#pipican:mobs/armored'], 'bosses': ['minecraft:wither', 'minecraft:ender_dragon', 'minecraft:warden', 'minecraft:elder_guardian'],
    # Gear set bonuses (tools/ore_builder/sets.py).
    'monsters': ['#pipican:mobs/monsters'], 'hostile mobs': ['#pipican:mobs/monsters'],
    'dragons': ['minecraft:ender_dragon'], 'the ender dragon': ['minecraft:ender_dragon'], 'wither': ['minecraft:wither'],
    'large creatures': ['#pipican:mobs/large'], 'large mobs': ['#pipican:mobs/large'],
    'fey and spirits': ['#pipican:mobs/spirits'], 'spirits': ['#pipican:mobs/spirits'],
    'demons': ['#pipican:mobs/nether'], 'demons and undead': ['#pipican:mobs/nether', '#minecraft:undead'],
}
MOB_TAGS = {
    'mobs/monsters': ['#minecraft:undead', '#minecraft:illager', '#minecraft:arthropod', 'minecraft:creeper', 'minecraft:witch',
                      'minecraft:enderman', 'minecraft:endermite', 'minecraft:silverfish', 'minecraft:slime', 'minecraft:magma_cube',
                      'minecraft:blaze', 'minecraft:ghast', 'minecraft:guardian', 'minecraft:elder_guardian', 'minecraft:shulker',
                      'minecraft:vex', 'minecraft:phantom', 'minecraft:piglin_brute', 'minecraft:hoglin', 'minecraft:zoglin',
                      'minecraft:ravager', 'minecraft:warden', 'minecraft:breeze', 'minecraft:creaking', 'minecraft:ender_dragon'],
    'mobs/large': ['minecraft:iron_golem', 'minecraft:ravager', 'minecraft:warden', 'minecraft:wither', 'minecraft:ender_dragon',
                   'minecraft:elder_guardian', 'minecraft:ghast', 'minecraft:hoglin', 'minecraft:zoglin', 'minecraft:sniffer',
                   'minecraft:polar_bear', 'minecraft:giant'],
    'mobs/spirits': ['minecraft:vex', 'minecraft:allay', 'minecraft:phantom', 'minecraft:ghast', 'minecraft:breeze', 'minecraft:creaking'],
    'mobs/nether': ['minecraft:blaze', 'minecraft:ghast', 'minecraft:magma_cube', 'minecraft:piglin', 'minecraft:piglin_brute',
                    'minecraft:zombified_piglin', 'minecraft:hoglin', 'minecraft:zoglin', 'minecraft:wither_skeleton', 'minecraft:strider',
                    'minecraft:wither'],
    'mobs/end': ['minecraft:enderman', 'minecraft:endermite', 'minecraft:shulker', 'minecraft:ender_dragon'],
    'mobs/unclean': ['#minecraft:undead', '#pipican:mobs/nether', '#pipican:mobs/end', 'minecraft:vex', 'minecraft:phantom'],
    'mobs/animals': ['minecraft:cow', 'minecraft:mooshroom', 'minecraft:pig', 'minecraft:sheep', 'minecraft:chicken', 'minecraft:horse',
                     'minecraft:donkey', 'minecraft:mule', 'minecraft:llama', 'minecraft:trader_llama', 'minecraft:rabbit', 'minecraft:fox',
                     'minecraft:wolf', 'minecraft:cat', 'minecraft:ocelot', 'minecraft:goat', 'minecraft:polar_bear', 'minecraft:panda',
                     'minecraft:bee', 'minecraft:turtle', 'minecraft:frog', 'minecraft:axolotl', 'minecraft:camel', 'minecraft:sniffer',
                     'minecraft:armadillo', 'minecraft:parrot', 'minecraft:hoglin'],
    'mobs/daylight': ['minecraft:zombie', 'minecraft:zombie_villager', 'minecraft:skeleton', 'minecraft:stray', 'minecraft:drowned',
                      'minecraft:phantom', 'minecraft:bogged'],
    'mobs/beasts': ['minecraft:wolf', 'minecraft:hoglin', 'minecraft:zoglin', 'minecraft:ravager', 'minecraft:polar_bear', 'minecraft:fox'],
    'mobs/neutral': ['minecraft:enderman', 'minecraft:piglin', 'minecraft:zombified_piglin', 'minecraft:wolf', 'minecraft:iron_golem',
                     'minecraft:bee', 'minecraft:polar_bear', 'minecraft:llama', 'minecraft:trader_llama', 'minecraft:panda', 'minecraft:dolphin',
                     'minecraft:goat', 'minecraft:spider', 'minecraft:cave_spider'],
    'mobs/mindless': ['minecraft:iron_golem', 'minecraft:snow_golem', 'minecraft:slime', 'minecraft:magma_cube', 'minecraft:shulker',
                      'minecraft:wither'],
    'mobs/armored': ['minecraft:iron_golem', 'minecraft:ravager', 'minecraft:warden', 'minecraft:wither', 'minecraft:armadillo'],
}
# Generated combination tags (name -> values); build.py writes these and MOB_TAGS.
GENERATED = {}


def _slug(s):
    return re.sub(r'[^a-z0-9]+', '_', s).strip('_')


def mobs(text):
    """"undead, nether mobs and vexes" -> a holder-set value (one tag/id, or a generated tag), or None."""
    text = text.strip()
    text = re.sub(r'^(?:all |any |the )?', '', text)
    text = re.sub(r'\s+mobs$', ' mobs', text)
    if text in MOB_GROUPS:
        parts = [text]
    else:
        parts = [p.strip() for p in re.split(r',\s*(?:and\s+|or\s+)?|\s+and\s+|\s+or\s+', text) if p.strip()]
    values = []
    for p in parts:
        p = re.sub(r'^(?:the )', 'the ', p) if p == 'wither' else p
        group = MOB_GROUPS.get(p) or MOB_GROUPS.get(p + ' mobs') or MOB_GROUPS.get(re.sub(r' mobs$', '', p))
        if group is None:
            return None
        values += [v for v in group if v not in values]
    if len(values) == 1:
        return values[0]
    if all(not v.startswith('#') for v in values):
        return values
    name = 'mobs/group_' + _slug('_'.join(parts))[:60]
    GENERATED[name] = values
    return '#pipican:' + name


VICTIM_STATES = [
    (r'(?:mobs |targets |enemies )?(?:below|under) (half|\d+(?:\.\d+)?%) health', lambda m: {'health': {'max': 0.5 if m.group(1) == 'half' else float(m.group(1)[:-1]) / 100}}),
    (r'(?:mobs |targets |enemies )?(?:above|over) (half|\d+(?:\.\d+)?%) health', lambda m: {'health': {'min': 0.5 if m.group(1) == 'half' else float(m.group(1)[:-1]) / 100}}),
    (r'(?:mobs |targets )?at full health', lambda m: {'health': {'min': 0.999}}),
    (r'(?:mobs |targets )?(?:with|that have|that has|affected by) (?:a |any )?harmful (?:potion )?effects?|affected mobs', lambda m: {'harmful_effect': True}),
    (r'(?:mobs |targets )?that (?:aren\'t|are not|weren\'t|were not|isn\'t|is not) targeting you|unaware mobs|mobs that don\'t see you',
     lambda m: {'not_targeting_you': True}),
    (r'(?:your |the )?marked (?:target|targets|mob|mobs|prey)|(?:your )?prey', lambda m: {'marked': True}),
    (r'burning mobs|mobs (?:that are )?on fire', lambda m: {'on_fire': True}),
    (r'cursed (?:mobs?|targets?|foes?)', lambda m: {'marked': True}),
    (r'scrutinised|scrutinized', lambda m: {'marked': True}),
    (r'unarmou?red (?:mobs?|targets?|enemies)', lambda m: {'unarmored': True}),
    (r'(?:your )?challenged foes?|(?:the |your )?(?:challenged|scrutinised|scrutinized) (?:mobs?|foes?|targets?)', lambda m: {'marked': True}),
    (r'(?:mobs|targets|enemies) (?:that are |who are )?targeting you', lambda m: {'targeting_you': True}),
    (r'(?:mobs|targets|enemies) within (\d+(?:\.\d+)?) blocks?(?: of you)?', lambda m: {'within': float(m.group(1))}),
    (r'(?:mobs|targets|enemies) (?:that are )?targeting (?:someone|somebody|anyone|something) else', lambda m: {'targeting_other': True}),
    (r'(?:mobs|targets|enemies) that glow for you|glowing (?:mobs|targets|enemies)', lambda m: {'effects': 'minecraft:glowing'}),
    (r'(poisoned|slowed|weakened|blinded|withered) (?:mobs|targets|enemies)', lambda m: {'effects': 'minecraft:' + {
        'poisoned': 'poison', 'slowed': 'slowness', 'weakened': 'weakness', 'blinded': 'blindness', 'withered': 'wither'}[m.group(1)]}),
    (r'bleeding (?:mobs|targets|enemies)|(?:mobs|targets) that are bleeding', lambda m: {'bleeding': True}),
]


def victim_state(text):
    t = text.strip()
    for pat, build in VICTIM_STATES:
        m = re.fullmatch(pat, t)
        if m:
            return build(m)
    return None


def _damage(incoming, pct, against=None, victim=None, weapons=None, dtype=None, scaling=None):
    e = {'type': 'pipican:damage_taken' if incoming else 'pipican:damage_dealt'}
    if dtype:
        e['damage_types'] = dtype
    if weapons:
        e['weapons'] = weapons
    if against:
        e['against'] = against
    if victim:
        e['victim'] = victim
    e['multiplier'] = round(1 + pct / 100, 6)
    if scaling:
        e['scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
    return e


def against(low, ctx, value, pct, scal):
    """"+10% damage to undead" / "+5% damage against mobs below half health" / "-15% damage taken from undead"."""
    if not pct:
        return None
    m = re.fullmatch(r'(?:(\w+) )?damage (?:to|against|vs\.?|versus|on hits against) (.+)', low)
    taken = re.fullmatch(r'(?:more |less )?damage taken from (.+)', low)
    if m and not taken:
        kind, what = m.group(1), m.group(2)
        if kind and kind not in ('melee', 'ranged', 'weapon', 'magic', 'fire'):
            return None
        state = victim_state(what)
        group = None if state else mobs(what)
        if state is None and group is None:
            return None
        v, s = scal(value)
        from effects import DAMAGE_TAGS
        dtype = DAMAGE_TAGS.get(kind) if kind in ('melee', 'magic', 'fire') else None
        weapons = '#pipican:weapons/ranged' if kind == 'ranged' else None
        return [_damage(False, v, against=group, victim=state, dtype=dtype, weapons=weapons, scaling=s)]
    if taken:
        group = mobs(taken.group(1))
        if group is None:
            return None
        v, s = scal(value * (-1 if low.startswith('less') else 1))
        return [_damage(True, v, against=group, scaling=s)]
    return None


def taken_from(low):
    """"takes 20% more damage from wither skeletons" / "axes deal +20% damage to you"."""
    m = re.fullmatch(r'takes? (\d+(?:\.\d+)?)% (more|less) damage from (.+?)(?: attacks)?', low)
    if m:
        group = mobs(m.group(3))
        if group is not None:
            return [_damage(True, float(m.group(1)) * (1 if m.group(2) == 'more' else -1), against=group)]
    m = re.fullmatch(r'(.+?) takes? ([+-]?\d+(?:\.\d+)?)% (?:more |extra )?damage from you', low)
    if m:
        group = mobs(m.group(1))
        if group is not None:
            return [_damage(False, float(m.group(2)), against=group)]
    m = re.fullmatch(r'(swords|axes|maces|spears|tridents|bows|crossbows) deal ([+-]\d+(?:\.\d+)?)% (?:more )?damage to you', low)
    if m:
        return [_damage(True, float(m.group(2)), weapons='#pipican:weapons/' + m.group(1))]
    m = re.fullmatch(r'(.+?) deal (\d+(?:\.\d+)?)% (more|less) damage to you', low)
    if m:
        group = mobs(m.group(1))
        if group is not None:
            return [_damage(True, float(m.group(2)) * (1 if m.group(3) == 'more' else -1), against=group)]
    return None


def critical(low, ctx, scal_fn):
    """"+10% critical damage", "critical hits deal +10% (extra) damage", "+5% crit chance"."""
    m = re.fullmatch(NUM + r'% (?:critical|crit) (?:hit )?damage', low) or \
        re.fullmatch(r'(?:critical|crit) hits deal ' + NUM + r'% (?:extra |more )?damage', low)
    if m:
        amount, scaling = scal_fn(float(m.group(1)))
        e = {'type': 'pipican:critical', 'damage': round(amount / 100, 6)}
        if scaling:
            e['scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
        return [e]
    m = re.fullmatch(NUM + r'% (?:critical|crit) (?:hit )?chance against (.+)', low)
    if m:
        state = victim_state(m.group(2))
        if state is None:
            mm = re.fullmatch(r'((?:slowed|blinded|poisoned|weakened|withered)(?:(?:, | or | and |, or )(?:slowed|blinded|poisoned|weakened|withered))*) mobs', m.group(2))
            if mm:
                names = {'slowed': 'slowness', 'blinded': 'blindness', 'poisoned': 'poison', 'weakened': 'weakness', 'withered': 'wither'}
                ids = ['minecraft:' + names[w] for w in re.split(r', or |, | or | and ', mm.group(1))]
                state = {'effects': ids if len(ids) > 1 else ids[0]}
        if state is None:
            return None
        amount, scaling = scal_fn(float(m.group(1)))
        e = {'type': 'pipican:critical', 'chance': round(amount / 100, 6), 'victim': state}
        if scaling:
            e['chance_scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
        return [e]
    m = re.fullmatch(NUM + r'% (?:critical|crit) (?:hit )?chance', low) or re.fullmatch(r'(?:critical|crit) (?:hit )?chance ' + NUM + '%', low)
    if m:
        amount, scaling = scal_fn(float(m.group(1)))
        e = {'type': 'pipican:critical', 'chance': round(amount / 100, 6)}
        if scaling:
            e['chance_scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
        return [e]
    return None


def dodge(low, ctx, scal_fn):
    """"+8% dodge chance", "8% chance to completely avoid a melee hit", "+5% chance a melee hit against you misses"."""
    m = re.fullmatch(r'\+?(\d+(?:\.\d+)?)% dodge(?: chance)?()', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% chance to (?:completely )?(?:avoid|dodge) (?:a |an )?(melee )?(?:hit|attack)s?', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% chance (?:a|an|any) (melee )?(?:hit|attack) against you misses', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% chance to parry (melee )?(?:hits|attacks)', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% chance attacks against you hit an illusion instead()', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% (?:miss chance|precognition)()', low) or \
        re.fullmatch(r'(?:miss chance|precognition) \+?(\d+(?:\.\d+)?)%()', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% (parry) chance', low) or re.fullmatch(r'(?:parry chance) \+?(\d+(?:\.\d+)?)%()', low) or \
        re.fullmatch(r'\+?(\d+(?:\.\d+)?)% (arrow)-?\s?deflect(?:ion)? chance', low) or re.fullmatch(r'arrow-?\s?deflect(?:ion)? chance \+?(\d+(?:\.\d+)?)%( arrow)', low)
    if not m:
        return None
    amount, scaling = scal_fn(float(m.group(1)))
    e = {'type': 'pipican:dodge', 'chance': round(amount / 100, 6)}
    if m.group(2) and 'arrow' in m.group(2):
        e['ranged_only'] = True
    elif m.group(2) or 'parry' in low:
        e['melee_only'] = True
    if scaling:
        e['scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
    return [e]


EFFECT_NAMES = {
    'poison': 'poison', 'wither': 'wither', 'the wither effect': 'wither', 'wither effect': 'wither', 'nausea': 'nausea',
    'slowness': 'slowness', 'weakness': 'weakness', 'blindness': 'blindness', 'darkness': 'darkness', 'hunger': 'hunger',
    'mining fatigue': 'mining_fatigue', 'levitation': 'levitation', 'bad omen': 'bad_omen', 'glowing': 'glowing',
    'speed': 'speed', 'strength': 'strength', 'regeneration': 'regeneration', 'resistance': 'resistance', 'haste': 'haste',
    'jump boost': 'jump_boost', 'slow falling': 'slow_falling', 'invisibility': 'invisibility', 'fire resistance': 'fire_resistance',
    'absorption': 'absorption', 'night vision': 'night_vision', 'water breathing': 'water_breathing', 'luck': 'luck',
    'instant damage': 'instant_damage', 'harming': 'instant_damage', 'instant health': 'instant_health',
    'unluck': 'unluck', 'infested': 'infested', 'oozing': 'oozing', 'weaving': 'weaving', 'wind charged': 'wind_charged',
}
ROMAN = {'i': 0, 'ii': 1, 'iii': 2, 'iv': 3, 'v': 4}


def immune(low):
    """"immune to nausea and blindness", "immune to knockback", "immune to freezing"."""
    m = re.fullmatch(r'(?:also )?(?:you are |you\'re |become )?immune to (.+)', low)
    if not m:
        return None
    out, effects = [], []
    for part in re.split(r',\s*(?:and\s+)?|\s+and\s+', re.sub(r',? and rotten flesh is normal food for you$', ', rotten flesh is normal food for you', m.group(1))):
        part = part.strip()
        part = re.sub(r'^(?:the )', '', part) if part.startswith('the ') and part not in EFFECT_NAMES else part
        part = re.sub(r' (?:effects?|damage)$', '', part) if part not in EFFECT_NAMES else part
        if part in EFFECT_NAMES:
            effects.append('minecraft:' + EFFECT_NAMES[part])
        elif part == 'knockback':
            out.append({'type': 'pipican:attribute', 'attribute': 'minecraft:knockback_resistance', 'amount': 1.0})
        elif part in ('fire', 'fire and lava', 'lava', 'burning'):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fire', 'multiplier': 0.0})
        elif part in ('freezing', 'freeze', 'powder snow'):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_freezing', 'multiplier': 0.0})
        elif part in ('fall', 'falls', 'fall damage'):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fall', 'multiplier': 0.0})
        elif part in ('drowning',):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_drowning', 'multiplier': 0.0})
        elif part in ('falling-block', 'falling block', 'falling blocks'):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_falling_block', 'multiplier': 0.0})
        elif part in ('food poisoning', 'rotten flesh is normal food for you'):
            out.append({'type': 'pipican:bonus', 'kind': 'food_safe', 'amount': 1.0})
        elif part in ('explosions',):
            out.append({'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_explosion', 'multiplier': 0.0})
        else:
            return None
    if effects:
        out.insert(0, {'type': 'pipican:immune', 'effects': effects if len(effects) > 1 else effects[0]})
    return out


# ------------------------------------------------------------------ reactions ("your hits have a 10% chance to ...")

def _effect_entry(name, seconds, level=None):
    e = {'effect': 'minecraft:' + EFFECT_NAMES[name], 'seconds': seconds}
    if level and ROMAN.get(level):
        e['amplifier'] = ROMAN[level]
    return e


def outcome(text, target):
    """What happens to `target` ("victim" or "self"): "give Wither I for 2s", "set them on fire", "knock them back"."""
    t = text.strip().rstrip('.')
    eff = r'(' + '|'.join(sorted((re.escape(k) for k in EFFECT_NAMES), key=len, reverse=True)) + r')(?: (i|ii|iii|iv|v))?'
    m = re.fullmatch(r'(?:give|gives|inflict|inflicts|apply|applies|cause|causes)(?: (?:it|them|the mob|the target|the attacker|mobs))? ' + eff +
                     r'(?: for (\d+(?:\.\d+)?)s| \((\d+(?:\.\d+)?)s\))?', t)
    if m:
        return [{'type': 'pipican:effect', 'target': target, 'effects': [_effect_entry(m.group(1), float(m.group(3) or m.group(4) or 3), m.group(2))]}]
    m = re.fullmatch(r'(?:gain|get|you gain|you get|grant you|give you) ' + eff + r'(?: for (\d+(?:\.\d+)?)s| \((\d+(?:\.\d+)?)s\))?', t)
    if m:
        return [{'type': 'pipican:effect', 'target': 'self', 'effects': [_effect_entry(m.group(1), float(m.group(3) or m.group(4) or 3), m.group(2))]}]
    m = re.fullmatch(r'(?:set|sets) (?:it|them|the mob|the target|the attacker|mobs) on fire(?: for (\d+)s)?|(?:catch|catches) fire|(?:ignite|ignites)(?: (?:it|them|the target|the attacker))?|be set on fire|be ignited', t)
    if m:
        return [{'type': 'pipican:damage', 'target': target, 'amount': 0, 'fire_seconds': float(m.group(1) or 4) if m.lastindex else 4.0}]
    # "give Blindness or Slowness for 2s": one of them at random
    m = re.fullmatch(r'(?:give|gives|inflict|inflicts|apply|applies)(?: (?:it|them|the mob|the target))? ' + eff + r' or ' + eff + r'(?: for (\d+(?:\.\d+)?)s)?', t)
    if m:
        secs = float(m.group(5) or 3)
        return [{'type': 'pipican:random', 'choices': [
            [{'type': 'pipican:effect', 'target': target, 'effects': [_effect_entry(m.group(1), secs, m.group(2))]}],
            [{'type': 'pipican:effect', 'target': target, 'effects': [_effect_entry(m.group(3), secs, m.group(4))]}]]}]
    m = re.fullmatch(r'(?:are|is|get|gets) set on fire(?: for (\d+(?:\.\d+)?)s)?', t)
    if m:
        return [{'type': 'pipican:damage', 'target': target, 'amount': 0, 'fire_seconds': float(m.group(1) or 4)}]
    m = re.fullmatch(r'chain (\d+(?:\.\d+)?) damage to (?:a |one )?nearby (?:mob|enemy)', t)
    if m:
        return [{'type': 'pipican:damage', 'target': 'near_victim', 'radius': 4.0, 'amount': float(m.group(1))}]
    m = re.fullmatch(r'drop (\d+) (?:extra )?experience orbs?(?: extra)?', t)
    if m:
        return [{'type': 'pipican:experience', 'amount': int(m.group(1))}]
    m = re.fullmatch(r'leave a lingering cloud of ' + eff, t)
    if m:
        c = {'type': 'pipican:cloud', 'effect': 'minecraft:' + EFFECT_NAMES[m.group(1)]}
        if m.group(2) and ROMAN.get(m.group(2)):
            c['amplifier'] = ROMAN[m.group(2)]
        return [c]
    m = re.fullmatch(r'(?:are|is|get|gets|become|becomes) (poisoned|slowed|weakened|blinded|withered)(?: for (\d+)s)?', t)
    if m:
        name = {'poisoned': 'poison', 'slowed': 'slowness', 'weakened': 'weakness', 'blinded': 'blindness', 'withered': 'wither'}[m.group(1)]
        return [{'type': 'pipican:effect', 'target': target, 'effects': [_effect_entry(name, float(m.group(2) or 3))]}]
    m = re.fullmatch(r'(?:knock|knocks) (?:it|them|the mob|the target|the attacker|mobs) back(?: (\d+(?:\.\d+)?) blocks)?', t)
    if m:
        blocks = float(m.group(1) or 3)
        return [{'type': 'pipican:push', 'target': target, 'strength': round(min(3.0, blocks / 3), 2)}]
    m = re.fullmatch(r'(?:heal|heals) you (?:for )?(\d+(?:\.\d+)?)(?: hp| health)?', t)
    if m:
        return [{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(1))}]
    m = re.fullmatch(r'(?:take|takes|suffer|suffers) (\d+(?:\.\d+)?) (magic |fire |lightning )?damage', t)
    if m:
        d = {'type': 'pipican:damage', 'target': target, 'amount': float(m.group(1))}
        if m.group(2) == 'fire ':
            d['fire_seconds'] = 3.0
        return [d]
    m = re.fullmatch(r'(?:deal|deals) (\d+(?:\.\d+)?) (?:extra |bonus )?(magic |fire )?damage', t)
    if m:
        d = {'type': 'pipican:damage', 'target': target, 'amount': float(m.group(1))}
        if m.group(2) == 'fire ':
            d['fire_seconds'] = 3.0
        return [d]
    return None


HIT_WORDS = {
    '': ('hit', None), 'melee ': ('melee_hit', None), 'arrow ': ('ranged_hit', None), 'ranged ': ('ranged_hit', None),
    'sword ': ('melee_hit', '#pipican:weapons/swords'), 'axe ': ('melee_hit', '#pipican:weapons/axes'),
    'mace ': ('melee_hit', '#pipican:weapons/maces'), 'spear ': ('melee_hit', '#pipican:weapons/spears'),
    'bow ': ('ranged_hit', '#pipican:weapons/bows'), 'crossbow ': ('ranged_hit', '#pipican:weapons/crossbows'),
}


def _reaction(event, chance, actions, weapons=None, victims=None):
    r = {'type': 'pipican:on', 'event': event}
    if chance < 1:
        r['chance'] = round(chance, 6)
    if weapons:
        r['weapons'] = weapons
    if victims:
        r['victims'] = victims
    r['actions'] = actions
    name = reaction_name(actions)
    if name and chance < 1 and event in ('hit', 'melee_hit', 'ranged_hit'):
        r['name'] = name
    return r


def reaction_name(actions):
    """A name chance upgrades can refer to ("+5% ignite chance"): what the first action does."""
    a = actions[0] if actions else {}
    if a.get('type') == 'pipican:effect' and a.get('effects'):
        return a['effects'][0]['effect'].split(':')[1]
    if a.get('type') == 'pipican:damage' and a.get('fire_seconds') and not a.get('amount'):
        return 'ignite'
    if a.get('type') == 'pipican:lightning':
        return 'lightning'
    if a.get('type') == 'pipican:control':
        return a.get('mode', 'control')
    return None


def reaction(low):
    """On-hit / on-kill / when-hit / on-block phrases -> [pipican:on]."""
    t = low.strip().rstrip('.')
    hit = r'(?:your )?(|melee |arrow |ranged |sword |axe |mace |spear |bow |crossbow )(?:hits|attacks|strikes|arrows)'
    m = re.fullmatch(r'(\d+(?:\.\d+)?)% chance (?:a |an )?(?:hostile )?mob you hit is charmed(?: - it stops attacking you for (\d+(?:\.\d+)?)s(?: and wanders off)?)?', t)
    if m:
        return [_reaction('hit', float(m.group(1)) / 100,
                          [{'type': 'pipican:control', 'target': 'victim', 'mode': 'pacify', 'seconds': float(m.group(2) or 5)}])]
    m = re.fullmatch(r'your hits have a (\d+(?:\.\d+)?)% chance (in thunderstorms|in (?:the )?rain|at night) to call down lightning on the (?:target|mob)', t) or \
        re.fullmatch(r'your hits have a (\d+(?:\.\d+)?)% chance()() to call down lightning on the (?:target|mob)', t)
    if m:
        r = _reaction('hit', float(m.group(1)) / 100, [{'type': 'pipican:lightning'}])
        when = {'in thunderstorms': 'thundering', 'in the rain': 'raining', 'in rain': 'raining', 'at night': 'night'}.get(m.group(2))
        if when:
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': when}, 'effects': [r]}]
        return [r]
    m = re.fullmatch(r'\+?(\d+(?:\.\d+)?)% chance (?:that )?your (|melee |ranged |arrow |sword |axe |bow |crossbow )hits (give|inflict|apply|cause) (.+)', t)
    if m:
        out = outcome(m.group(3) + ' ' + m.group(4), 'victim')
        if out:
            event, weapons = HIT_WORDS[m.group(2)]
            return [_reaction(event, float(m.group(1)) / 100, out, weapons)]
    m = re.fullmatch(r'(\d+(?:\.\d+)?)% chance on (|melee |ranged |arrow |sword |axe |bow |crossbow )hit to (.+)', t)
    if m:
        out = outcome(m.group(3), 'victim')
        if out:
            event, weapons = HIT_WORDS[m.group(2)]
            return [_reaction(event, float(m.group(1)) / 100, out, weapons)]
    m = re.fullmatch(hit + r' have (?:a )?(\d+(?:\.\d+)?)% chance to (.+)', t) or \
        re.fullmatch(r'(\d+(?:\.\d+)?)% chance (?:that |for )?(?:your )?(|melee |arrow |ranged |sword |axe |bow |crossbow )(?:hits?|attacks?) (?:also )?(.+)', t)
    if m:
        if m.re.pattern.startswith(hit):
            kind, chance, rest = m.group(1), float(m.group(2)) / 100, m.group(3)
        else:
            chance, kind, rest = float(m.group(1)) / 100, m.group(2), m.group(3)
        out = outcome(rest, 'victim')
        if out:
            event, weapons = HIT_WORDS[kind]
            return [_reaction(event, chance, out, weapons)]
    m = re.fullmatch(hit + r' (?:also )?(.+)', t)
    if m and m.group(1) in HIT_WORDS:
        out = outcome(m.group(2), 'victim')
        if out:
            event, weapons = HIT_WORDS[m.group(1)]
            return [_reaction(event, 1.0, out, weapons)]
    m = re.fullmatch(r'(?:mobs|enemies|attackers|anything) that (?:hit|hits|attack|attacks|strike|strikes|melee|melees) you (?:have (?:a )?(\d+(?:\.\d+)?)% chance to )?(.+)', t)
    if m:
        out = outcome(m.group(2), 'victim')
        if out:
            return [_reaction('hurt', float(m.group(1)) / 100 if m.group(1) else 1.0, out)]
    m = re.fullmatch(r'(?:hitting|striking|attacking) (?:a mob|mobs|an enemy) from behind (.+)', t)
    if m:
        rest = re.sub(r'^(?:gives|inflicts|applies) (?:it|them) ', 'give ', m.group(1))
        out = outcome(rest, 'victim')
        if out:
            r = _reaction('hit', 1.0, out)
            r['victim'] = {'from_behind': True}
            return [r]
    m = re.fullmatch(r'(?:hostile )?mobs you kill have (?:a )?(\d+(?:\.\d+)?)% chance to (.+)', t)
    if m:
        out = outcome(m.group(2), 'victim')
        if out:
            return [_reaction('kill', float(m.group(1)) / 100, out)]
    m = re.fullmatch(r'(.+?) you hit (?:are|is|get|gets) (.+)', t)
    if m and m.group(1) not in ('mobs', 'enemies', 'targets'):
        group = mobs(m.group(1))
        rest = ('are ' + m.group(2)) if m.group(2).startswith('set on fire') else m.group(2)
        out = outcome(rest, 'victim') if group else None
        if out:
            return [_reaction('hit', 1.0, out, victims=group)]
    m = re.fullmatch(r'(?:kills|killing a mob|killing mobs|each kill|every kill)(?: also)? (?:gives?|grants?)(?: you)? (.+)', t) or \
        re.fullmatch(r'(?:when|whenever|after) you kill (?:a mob|mobs|something),? (?:you )?(.+)', t) or \
        re.fullmatch(r'after a kill,? (.+)', t)
    if m:
        rest = m.group(1)
        out = outcome(rest, 'self') or outcome('gain ' + rest, 'self')
        if out:
            return [_reaction('kill', 1.0, out)]
    m = re.fullmatch(r'blocking (?:a |an )?(?:melee )?(?:attack|hit)s? (?:with a shield )?(.+)', t)
    if m:
        rest = re.sub(r'^(?:also )?', '', m.group(1))
        out = outcome(rest, 'victim') or outcome('gain ' + re.sub(r'^gives? (?:you )?', '', rest), 'self')
        if out:
            return [_reaction('block', 1.0, out)]
    return None

"""Translate plain-language bonus phrases into pipican effect JSON.

translate(text, ctx) -> (effects, unsupported_clauses)
ctx: {'special_name': 'Grit', 'special_id': 'pipican:warrior_grit', 'implicit_per_point': bool}
"""
import re

import bonus
import named
import combat
import conditions as cond

CORE_STATS = ['vitality', 'strength', 'dexterity', 'agility', 'intelligence', 'wisdom', 'endurance', 'luck']

NUM = r'([+-]?\d+(?:\.\d+)?)'

WEAPON_TAGS = {
    'sword': ['#pipican:weapons/swords'], 'swords': ['#pipican:weapons/swords'],
    'axe': ['#pipican:weapons/axes'], 'axes': ['#pipican:weapons/axes'],
    'mace': ['#pipican:weapons/maces'], 'maces': ['#pipican:weapons/maces'],
    'spear': ['#pipican:weapons/spears'], 'spears': ['#pipican:weapons/spears'],
    'trident': ['#pipican:weapons/tridents'], 'tridents': ['#pipican:weapons/tridents'],
    'bow': ['#pipican:weapons/bows'], 'bows': ['#pipican:weapons/bows'],
    'crossbow': ['#pipican:weapons/crossbows'], 'crossbows': ['#pipican:weapons/crossbows'],
    'shield': ['#pipican:weapons/shields'],
    'bow and crossbow': ['#pipican:weapons/bows', '#pipican:weapons/crossbows'],
    'bows and crossbows': ['#pipican:weapons/bows', '#pipican:weapons/crossbows'],
    'axe and mace': ['#pipican:weapons/axes', '#pipican:weapons/maces'],
    'sword and axe': ['#pipican:weapons/swords', '#pipican:weapons/axes'],
    'staff': ['#pipican:weapons/staff'], 'staffs': ['#pipican:weapons/staff'], 'staff of magic': ['#pipican:weapons/staff'],
    'iron, diamond and netherite weapons': ['#pipican:weapons/metal'],
    'firearm': ['#pipican:weapons/firearms'], 'firearms': ['#pipican:weapons/firearms'], 'gun': ['#pipican:weapons/firearms'],
    'pickaxe': ['#minecraft:pickaxes'], 'pickaxes': ['#minecraft:pickaxes'],
    'ranged': ['#pipican:weapons/ranged'], 'ranged weapons': ['#pipican:weapons/ranged'], 'melee weapons': ['#pipican:weapons/melee'],
}
# Damage-type tags. pipican:* ones are shipped by this build (see DAMAGE_TYPE_TAGS).
DAMAGE_TAGS = {
    'fire': '#minecraft:is_fire', 'fall': '#minecraft:is_fall', 'projectile': '#minecraft:is_projectile',
    'explosion': '#minecraft:is_explosion', 'explosive': '#minecraft:is_explosion', 'freezing': '#minecraft:is_freezing',
    'drowning': '#minecraft:is_drowning', 'lightning': '#minecraft:is_lightning',
    'magic': '#pipican:is_magic', 'wither': '#pipican:is_wither', 'melee': '#pipican:is_melee', 'poison': '#pipican:is_magic',
    'ranged': '#minecraft:is_projectile', 'suffocation': '#pipican:is_suffocation', 'plant': '#pipican:is_plant', 'falling-block': '#pipican:is_falling_block',
    'falling block': '#pipican:is_falling_block', 'falling blocks': '#pipican:is_falling_block',
}
DAMAGE_TYPE_TAGS = {
    'is_mace_smash': ['minecraft:mace_smash'],
    'is_magic': ['minecraft:magic', 'minecraft:indirect_magic', 'minecraft:sonic_boom', 'minecraft:thorns'],
    'is_wither': ['minecraft:wither', 'minecraft:wither_skull'],
    'is_melee': ['minecraft:player_attack', 'minecraft:mob_attack', 'minecraft:mob_attack_no_aggro'],
    'is_suffocation': ['minecraft:in_wall', 'minecraft:cramming'],
    'is_plant': ['minecraft:cactus', 'minecraft:sweet_berry_bush'],
    'is_ender_pearl': ['minecraft:ender_pearl'],
    'is_falling_block': ['minecraft:falling_block', 'minecraft:falling_anvil', 'minecraft:falling_stalactite'],
}

# attribute phrase -> (attribute id, kind) ; kind 'flat' = amount as written, 'pct_base' = percent of base,
# 'pct_flat' = percent written as fraction added (0-1 attributes)
ATTRS = {
    'max health': ('minecraft:max_health', 'flat'),
    'health': ('minecraft:max_health', 'flat'),
    'armor': ('minecraft:armor', 'flat'),
    'armour': ('minecraft:armor', 'flat'),
    'armor toughness': ('minecraft:armor_toughness', 'flat'),
    'toughness': ('minecraft:armor_toughness', 'flat'),
    'attack damage': ('minecraft:attack_damage', 'flat'),
    'reach': ('minecraft:entity_interaction_range', 'flat'),
    'jump strength': ('minecraft:jump_strength', 'flat'),
    'oxygen': ('minecraft:oxygen_bonus', 'flat'),
    'underwater mining speed': ('minecraft:submerged_mining_speed', 'flat'),
    'attack reach': ('minecraft:entity_interaction_range', 'flat'),
    'block reach': ('minecraft:block_interaction_range', 'flat'),
    'safe-fall blocks': ('minecraft:safe_fall_distance', 'flat'),
    'safe fall distance': ('minecraft:safe_fall_distance', 'flat'),
    'step height': ('minecraft:step_height', 'flat'),
    'movement speed': ('minecraft:movement_speed', 'pct_base'),
    'move speed': ('minecraft:movement_speed', 'pct_base'),
    'speed': ('minecraft:movement_speed', 'pct_base'),
    'attack speed': ('minecraft:attack_speed', 'pct_base'),
    'mining speed': ('minecraft:block_break_speed', 'pct_base'),
    'jump height': ('minecraft:jump_strength', 'pct_base'),
    'knockback resistance': ('minecraft:knockback_resistance', 'pct_flat'),
    'knockback': ('minecraft:attack_knockback', 'pct_base'),
    'melee knockback': ('minecraft:attack_knockback', 'pct_base'),
    'swim speed': ('minecraft:water_movement_efficiency', 'pct_flat'),
    'oxygen': ('minecraft:oxygen_bonus', 'flat'),
    'underwater breathing': ('minecraft:oxygen_bonus', 'flat'),
    'scale': ('minecraft:scale', 'flat'),
    'size': ('minecraft:scale', 'pct_base'),
    'sneaking speed': ('minecraft:sneaking_speed', 'pct_base'),
    'experience gained': None,
}

MOB_EFFECTS = {
    'night vision': 'minecraft:night_vision', 'water breathing': 'minecraft:water_breathing',
    'fire resistance': 'minecraft:fire_resistance', 'haste': 'minecraft:haste', 'speed': 'minecraft:speed',
    'strength': 'minecraft:strength', 'resistance': 'minecraft:resistance', 'jump boost': 'minecraft:jump_boost',
    'regeneration': 'minecraft:regeneration', 'luck': 'minecraft:luck', 'conduit power': 'minecraft:conduit_power',
    'dolphins grace': 'minecraft:dolphins_grace', "dolphin's grace": 'minecraft:dolphins_grace', 'slow falling': 'minecraft:slow_falling',
}
# Damage players only ever take, so "-30% fall damage" means less fall damage taken.
TAKEN_ONLY = {'fall', 'drowning', 'freezing'}
# A phrase containing one of these is conditional: never apply part of it on its own.
CONDITION = re.compile(r'\b(while|when|whenever|without|with no|if|unless|during|after|before|against|vs|underground|at night|'
                       r'in daylight|daylight|the nether|the end|outdoors|indoors|inside|near|nearby|within|below|above|in water|'
                       r'rain|wet|mounted|riding|sneaking|sprinting|on hit|chance|first|each time|every time|to mobs|to undead|'
                       r'from \w+ mobs|holding|wearing|light level|darkness|in combat|unaware|marked|prey|cursed|poisoned|burning|'
                       r'transformed|berserk|stance|aura|full|empty|biomes?|forest|village|until|for \d+s|\d+s|toggle|drains|'
                       r'spend|active|tamed|pets?|mounts?|allies|minions?|summon\w*|eidolon|servants?|players)\b', re.I)
# A "Label:" prefix ending in one of these names who/what gets the bonus ("Sigil weapons:"), not a talent name.
SUBJECT = re.compile(r'(?:weapons|armou?r|mounts|pets|minions|allies|summons|servants|sigils|runes|gadgets|decoys|eidolon|shields|bows|'
                     r'swords|axes|maces|spears|tridents|crossbows|firearms|animals|beasts|songs|auras)$', re.I)
ROMAN = {'i': 0, 'ii': 1, 'iii': 2, 'iv': 3}


def _num(s):
    return float(s.replace('+', ''))


def _attr_effect(attr, kind, value, scaling=None):
    e = {'type': 'pipican:attribute', 'attribute': attr}
    if kind == 'flat':
        amount, op = value, 'add_value'
    elif kind == 'pct_base':
        amount, op = value / 100, 'add_multiplied_base'
    else:
        amount, op = value / 100, 'add_value'
    e['amount'] = round(amount, 6)
    if op != 'add_value':
        e['operation'] = op
    if scaling:
        e['scaling'] = [{'source': s, 'per': round(p if kind == 'flat' else p / 100, 6)} for s, p in scaling]
    return e


def _dmg_effect(incoming, pct, weapons=None, dtype=None, scaling=None):
    e = {'type': 'pipican:damage_taken' if incoming else 'pipican:damage_dealt'}
    if dtype:
        e['damage_types'] = dtype
    if weapons:
        e['weapons'] = weapons[0] if len(weapons) == 1 else None
    e['multiplier'] = round(1 + pct / 100, 6)
    if scaling:
        e['scaling'] = [{'source': s, 'per': round(p / 100, 6)} for s, p in scaling]
    return e


def _clean(clause):
    c = clause.strip().strip('.').strip()
    c = c.replace('−', '-').replace('–', '-')
    c = re.sub(r'\s+', ' ', c)
    # Drop a "Name: " label prefix and trailing "(Label)" annotations.
    label = re.match(r'^([A-Z][\w\' -]{1,30}):\s+(?=\S)', c)
    if label and not SUBJECT.search(label.group(1)):
        c = c[label.end():]
    c = re.sub(r'\s*\((?:[A-Z][^)]*|book[^)]*)\)\s*$', '', c)
    c = re.sub(r'^but\s+', '', c, flags=re.I)
    c = re.sub(r'^([+-]?\d+(?:\.\d+)?%) more (?=\w*\s*damage)', r'\1 ', c)
    c = re.sub(r'\s+everywhere(?: else)?$', '', c)
    return c.strip()


def _split(text):
    text = re.sub(r',?\s+but\s+', ', but ', text)
    parts = []
    for chunk in text.split(';'):
        # Commas only start a new clause when what follows looks like one ("+2 armor, -5% speed");
        # otherwise they are part of a list ("swords, bows and crossbows").
        pieces = []
        for piece in re.split(r',\s*', chunk):
            if pieces and not re.match(r'^\s*(?:but\s+)?(?:[+-]?\d|takes?\b|immune|mobs deal|no\b)', piece):
                pieces[-1] += ', ' + piece
            else:
                pieces.append(re.sub(r'^\s*but\s+', '', piece))
        parts.extend(pieces)
    out = []
    for p in parts:
        # "+2 armor and +1 toughness" -> two clauses; keep "bow and crossbow" together
        out.extend(re.split(r'\s+and\s+(?=[+-]\d|takes? \d|mobs deal|no\b)', p))
    return [p for p in out if p.strip()]


def _stat_ids(name, ctx):
    n = name.lower().strip()
    if n in CORE_STATS:
        return ['pipican:' + n]
    if ctx.get('special_name') and n == ctx['special_name'].lower():
        return [ctx['special_id']]
    if n in ('every stat', 'all stats', 'every core stat', 'each stat', 'all your stats'):
        return ['pipican:' + s for s in CORE_STATS]
    return None


def _scaling(rest, ctx):
    """Parse a trailing scaling phrase. Returns ([(source, per_unit)], remainder) or (None, rest)."""
    sp = (ctx.get('special_name') or '').lower()
    m = re.search(r'\s*(?:,\s*)?(?:\+?\s*(?:[\d.]+\s*)?more\s+)?(?:per|for every|for each|every)\s+(?:(\d+)\s+)?(class levels?|levels?|[a-z][a-z \'-]*?)(?:\s+points?)?\s*$', rest, re.I)
    if not m:
        return None, rest
    every = float(m.group(1)) if m.group(1) else 1.0
    src = m.group(2).lower().strip()
    if src in ('class level', 'class levels', 'level', 'levels'):
        source = 'class_level'
    elif sp and src == sp:
        source = 'special_stat'
    elif src in CORE_STATS:
        source = 'pipican:' + src
    elif src in ('special-stat point', 'special stat'):
        source = 'special_stat'
    elif src in ('sigil piece', 'sigil armor piece', 'sigil-marked armor piece', 'piece of sigil armor'):
        source = 'sigil_armor'
    elif src in ('heavy armor piece', 'heavy piece', 'heavy armor piece worn', 'piece of heavy armor'):
        source = 'heavy_armor'
    elif src in ('missing heart',):
        source = 'missing_hearts'
    elif src in ('missing health percent',):
        source = 'missing_health_percent'
    else:
        return None, rest
    return (source, every), rest[:m.start()].strip()


def _one(clause, ctx):
    """Translate one clause. Returns list of effects or None if unsupported."""
    out = _one_inner(clause, ctx)
    if out is not None:
        return out
    # "+20% attack speed and reload speed": one number for two bonuses.
    m = re.fullmatch(r'\s*([+-]\d+(?:\.\d+)?%?) ([a-z][a-z \'-]*?) and ([a-z][a-z \'-]*?)(\s+(?:per|for every)\b.*)?\s*', _clean(clause).lower())
    if m and not re.search(r'\d', m.group(3)):
        tail = m.group(4) or ''
        a = _one_inner(f'{m.group(1)} {m.group(2)}{tail}', ctx)
        b = _one_inner(f'{m.group(1)} {m.group(3)}{tail}', ctx)
        if a and b:
            return a + b
    # "+20% damage and knockback immunity": two whole bonuses (no shared "per" tail to share out).
    low = _clean(clause).lower()
    if ' and ' in low and not re.search(r'\bper\b|for every', low):
        for mm in re.finditer(r' and ', low):
            a = _one_inner(low[:mm.start()], ctx)
            b = _one_inner(low[mm.end():], ctx) if a else None
            if a and b:
                return a + b
    return None


def _one_inner(clause, ctx):
    c = _clean(clause)
    low = c.lower()

    # Separate scaling ("... per 2 Grit", "+1% more every 10 class levels")
    base_val = None
    scaling = None
    m = re.match(r'^(.*?)(?:,\s*)?\+?(\d+(?:\.\d+)?)%?\s+more\s+(?:every|per)\s+(\d+)\s+(class levels?|levels?)$', low)
    if m:  # "+5% weapon damage, +1% more every 10 class levels" (second half arrives as its own clause)
        pass
    sc, rest = _scaling(low, ctx)
    implicit = False
    if sc is None and ctx.get('implicit_per_point'):
        implicit = True
    if sc is not None:
        source, every = sc
        low = rest
    eid = _eidolon(low, ctx)
    if eid is not None:
        return eid
    res = _resource(low, ctx)
    if res is not None:
        target, value, pct = res
        if sc is not None:
            amount, scaling = 0.0, [{'source': source, 'per': round((value / 100 if pct else value) / every, 6)}]
        elif implicit:
            amount, scaling = 0.0, [{'source': 'special_stat', 'per': round(value / 100 if pct else value, 6)}]
        else:
            amount, scaling = round(value / 100 if pct else value, 6), None
        e = {'type': 'pipican:resource', 'target': target, 'amount': amount}
        if pct:
            e['operation'] = 'add_multiplied_base'
        if scaling:
            e['scaling'] = scaling
        return [e]
    def scal(v):
        if sc is not None:
            return 0.0, [(source, v / every)]
        if implicit:
            return 0.0, [('special_stat', v)]
        return v, None

    # "+0.5% weapon damage" etc
    mval = re.match(r'^' + NUM + r'(%?)\s+(.*)$', low)
    if not mval:
        return _special_forms(low, ctx, scal)
    value = _num(mval.group(1))
    pct = mval.group(2) == '%'
    what = mval.group(3).strip()
    what = re.sub(r'\s+(?:with any weapon|at all times|permanently|always|you deal|dealt yourself|dealt with all weapons|dealt)$', '', what)
    what = re.sub(r'^to\s+', '', what)

    whole = f'{mval.group(1)}{"%" if pct else ""} {what}'
    for special in (combat.critical(whole, ctx, scal), combat.dodge(whole, ctx, scal)):
        if special is not None:
            return special
    if pct:
        bn = bonus.leading(value, what, scal)
        if bn is not None:
            return bn
    ch = bonus.chances(whole, scal) or named.summons(whole, scal) or named.pets(whole, scal)
    if ch:
        return ch
    vs = combat.against(what, ctx, value, pct, scal)
    if vs is not None:
        return vs

    # stats
    st = _stat_ids(re.sub(r'\s+points?$', '', what), ctx)
    if st and not pct:
        if sc is not None or implicit:
            return None  # stat scaling isn't supported by pipican:stat
        return [{'type': 'pipican:stat', 'stat': s, 'amount': int(value) if value == int(value) else value} for s in st]

    # damage taken
    m = re.match(r'^(?:more |less )?(?:(\w+(?: and \w+)?) )?damage taken(?: from (\w+))?$', what)
    if pct and m:
        dt = m.group(1) or m.group(2)
        dtag = None
        if dt:
            dtag = DAMAGE_TAGS.get(dt)
            if not dtag:
                return None
        sign = -1 if what.startswith('less') else 1
        v, s = scal(value * sign)
        return [_dmg_effect(True, v, dtype=dtag, scaling=s)]
    # damage dealt
    m = re.match(r'^(?:(.+?) )?damage(?: with (.+?))?(?: dealt)?$', what)
    if pct and m:
        qual = (m.group(1) or '').strip()
        # Elemental damage: a bonus of "-50% fire damage" means taking less; a weakness of "+25% fire damage" means
        # taking more. "+30% fire damage" as a bonus (or "-10% magic damage" as a weakness) is damage dealt.
        elemental = qual in DAMAGE_TAGS and qual not in ('melee', 'ranged') and not m.group(2)
        if qual in TAKEN_ONLY or (elemental and (value > 0) == bool(ctx.get('weakness'))):
            v, s = scal(value)
            return [_dmg_effect(True, v, dtype=DAMAGE_TAGS[qual], scaling=s)]
        withq = (m.group(2) or '').strip()
        weapons = None
        dtag = None
        for q in [x for x in (qual, withq) if x]:
            q = re.sub(r'^(?:all |any |your )', '', q)
            if q in ('weapon', 'weapons', 'all weapons', 'any weapon', 'all', ''):
                continue
            if q in WEAPON_TAGS:
                weapons = WEAPON_TAGS[q]
            elif q in DAMAGE_TAGS:
                dtag = DAMAGE_TAGS[q]
            elif ' and ' in q or ',' in q:
                # "melee and ranged", "axes and maces", "explosion and magic": one effect per part
                v, s = scal(value)
                out = []
                for part in re.split(r',\s*|\s+and\s+', q):
                    part = part.strip()
                    if part in WEAPON_TAGS:
                        out += [dict(_dmg_effect(False, v, scaling=s), weapons=w) for w in WEAPON_TAGS[part]]
                    elif part in DAMAGE_TAGS:
                        out.append(_dmg_effect(False, v, dtype=DAMAGE_TAGS[part], scaling=s))
                    else:
                        return None
                return out
            else:
                return None
        if weapons and len(weapons) > 1:
            v, s = scal(value)
            return [dict(_dmg_effect(False, v, scaling=s), weapons=w) for w in weapons]
        v, s = scal(value)
        e = _dmg_effect(False, v, weapons=weapons, dtype=dtag, scaling=s)
        if e.get('weapons') is None:
            e.pop('weapons', None)
        return [e]
    # attributes
    key = what
    key = re.sub(r'^max\s+health$', 'max health', key)
    if key in ATTRS and ATTRS[key]:
        attr, kind = ATTRS[key]
        if pct and kind == 'flat':
            if key in ('max health', 'health', 'armor', 'armour'):
                kind = 'pct_base'
            else:
                return None
        if not pct and kind != 'flat':
            return None
        v, s = scal(value)
        return [_attr_effect(attr, kind, v, s)]
    return None


EIDOLON_ATTRS = {'max health': ('minecraft:max_health', 'flat'), 'health': ('minecraft:max_health', 'flat'), 'armor': ('minecraft:armor', 'flat'),
                 'damage': ('minecraft:attack_damage', 'pct_base'), 'movement speed': ('minecraft:movement_speed', 'pct_base'),
                 'speed': ('minecraft:movement_speed', 'pct_base'), 'armor toughness': ('minecraft:armor_toughness', 'flat')}


def _eidolon(low, ctx):
    """"eidolon +10 max health", "+1% eidolon damage and max health" -> pipican:eidolon effects, or None."""
    m = re.match(r'^eidolon ' + NUM + r'(%?) (.+)$', low) or re.match(r'^' + NUM + r'(%?) eidolon (.+)$', low)
    if not m:
        return None
    value, pct = _num(m.group(1)), m.group(2) == '%'
    out = []
    for part in re.split(r'\s+and\s+', m.group(3)):
        spec = EIDOLON_ATTRS.get(part.strip())
        if spec is None:
            return None
        attribute, kind = spec
        if pct and kind == 'flat':
            kind = 'pct_base'
        if not pct and kind != 'flat':
            return None
        amount = value / 100 if pct else value
        e = {'type': 'pipican:eidolon', 'attribute': attribute, 'amount': round(amount, 6)}
        if kind == 'pct_base':
            e['operation'] = 'add_multiplied_base'
        if ctx.get('implicit_per_point'):
            e['amount'] = 0.0
            e['scaling'] = [{'source': 'special_stat', 'per': round(amount, 6)}]
        out.append(e)
    return out


def _resource(low, ctx):
    """Resource-bar phrases -> (target, value, is_percent), or None."""
    rn = (ctx.get('resource_name') or '').lower()
    if not rn:
        return None
    # "Breath of Wind" / "Mystic Flux": lines often say just "Breath" or "Flux".
    words = rn.split()
    names = {rn, words[0], words[-1]} if len(words) > 1 and words[0] not in ('the', 'a') else {rn}
    r = '(?:' + '|'.join(sorted((re.escape(n) for n in names), key=len, reverse=True)) + ')'
    regen = r'regen(?:eration)?(?: per second|/s| speed| rate)?'
    for pat, target in ((rf'^{NUM}(%?) {r} {regen}$', 'regen'), (rf'^{r} {regen} {NUM}(%?)$', 'regen'),
                        (rf'^{NUM}(%?) (?:max(?:imum)? {r}|{r} max(?:imum)?)$', 'max'), (rf'^max(?:imum)? {r} {NUM}(%?)$', 'max'),
                        (rf'^{r} regenerates {NUM}(%) faster$', 'regen'), (rf'^{r} drains {NUM}(%) faster$', 'regen')):
        m = re.match(pat, low)
        if m:
            return target, _num(m.group(1)), m.group(2) == '%'
    if re.match(rf'^no {r} {regen}$', low):
        return 'regen', -100.0, True
    if re.match(rf'^{r} (?:regen(?:eration)? doubles|regenerates twice as fast)$', low):
        return 'regen', 100.0, True
    return None


def _special_forms(low, ctx, scal=lambda v: (v, None)):
    for special in (combat.immune(low), combat.taken_from(low), combat.critical(low, ctx, scal), combat.dodge(low, ctx, scal), named.pets(low, scal)):
        if special is not None:
            return special
    bn = bonus.special(low, scal)
    if bn is not None:
        return bn
    m = re.match(r'^mobs deal (\d+(?:\.\d+)?)% (more|less) damage to you$', low)
    if m:
        return [_dmg_effect(True, _num(m.group(1)) * (1 if m.group(2) == 'more' else -1))]
    m = re.match(r'^takes? (\d+(?:\.\d+)?)% (more|less) (\w+)(?: and (\w+))? damage$', low)
    if m:
        out = []
        for dt in [m.group(3), m.group(4)]:
            if not dt:
                continue
            tag = DAMAGE_TAGS.get(dt)
            if not tag:
                return None
            out.append(_dmg_effect(True, _num(m.group(1)) * (1 if m.group(2) == 'more' else -1), dtype=tag))
        return out
    m = re.match(r'^takes? (\d+(?:\.\d+)?)% (more|less) damage(?: from (\w+)(?: attacks)?)?$', low)
    if m:
        tag = None
        if m.group(3):
            tag = DAMAGE_TAGS.get(m.group(3))
            if not tag:
                return None
        return [_dmg_effect(True, _num(m.group(1)) * (1 if m.group(2) == 'more' else -1), dtype=tag)]
    m = re.match(r'^(?:permanent |constant |always have )?([a-z\' ]+?)(?: (i|ii|iii|iv))?(?: permanently| at all times)?$', low)
    if m and m.group(1).strip() in MOB_EFFECTS and (low.startswith(('permanent', 'constant', 'always')) or m.group(2) or m.group(1) in ('night vision', 'water breathing', 'fire resistance', 'conduit power')):
        e = {'type': 'pipican:mob_effect', 'effect': MOB_EFFECTS[m.group(1).strip()]}
        if m.group(2) and ROMAN[m.group(2)]:
            e['amplifier'] = ROMAN[m.group(2)]
        return [e]
    return None


def _bulk(text):
    """Phrasing rewrites, applied to the whole line and again to each part of it."""
    lab = r"^((?:[A-Z][\w' -]{1,30}:\s+)?)"
    num = r'([+-]?\d+(?:\.\d+)?)'
    subs = [
        (lab + r'\+?(\d+(?:\.\d+)?)% less damage from (?:all )?(?:mobs|enemies|attacks)$', r'\1-\2% damage taken'),
        (lab + r'fall damage halved(?: again)?$', r'\1-50% fall damage taken'),
        (lab + r'arrows shot at you are (\d+(?:\.\d+)?)% likely to be blown aside$', r'\1+\2% arrow-deflect chance'),
        (lab + r'you deal ([+-]\d+(?:\.\d+)?)% (?:more )?damage(?: yourself)?$', r'\1\2% damage'),
        (lab + r'you take ([+-]\d+(?:\.\d+)?%) (?:more )?damage\b', r'\1\2 damage taken'),
        (lab + r'your own damage ([+-]\d+(?:\.\d+)?%)$', r'\1\2 damage'),
        (lab + r'(?:but )?you get ([+-]\d+(?:\.\d+)?%? [a-z ]+)$', r'\1\2'),
        (lab + r'([+-]\d+(?:\.\d+)?%) ([a-z ]+?) for (?:other )?(players|allies) near you$', r'\1\4 near you +\2 \3'),
        (lab + r'full ([A-Z][\w]+(?: [A-Z][\w]+)?) gives ([+-]\d+(?:\.\d+)?%? [a-z ]+)$', r'\1\3 while \2 is full'),
        (lab + r'([+-]\d+(?:\.\d+)?) more (armou?r|armor toughness)\b', r'\1\2 \3'),
        (lab + r'(without [a-z ]+?),? you deal ([+-]\d+(?:\.\d+)?%) (?:more )?damage$', r'\1\3 damage \2'),
        (lab + r'(?:you )?takes? (\d+(?:\.\d+)?)% (more|less) damage from weapons and projectiles$', r'\1takes \2% \3 melee and projectile damage'),
        (lab + r'\+(\d+(?:\.\d+)?)% projectile damage reduction$', r'\1takes \2% less projectile damage'),
        (lab + r'\+(\d+(?:\.\d+)?)% damage reduction\b', r'\1-\2% damage taken'),
        (lab + r'-(\d+(?:\.\d+)?)% damage from (?:all other )?explosions$', r'\1takes \2% less explosion damage'),
        (lab + r'take -(\d+(?:\.\d+)?)% damage from tnt traps$', r'\1takes \2% less explosion damage'),
        (lab + r'-(\d+(?:\.\d+)?)% suffocation damage$', r'\1takes \2% less suffocation damage'),
        (lab + r"berry bushes and cacti don't hurt you$", r'\1takes 100% less plant damage'),
        (lab + r'takes (\d+)% more damage from (.+?),? and the wither effect$', r'\1takes \2% more damage from \3; takes \2% more wither damage'),
        (lab + r'-(\d+(?:\.\d+)?)% damage from ((?!all\b)[a-z][a-z, ]+)$', r'\1takes \2% less damage from \3'),
        (lab + r'([+-]\d+(?:\.\d+)?%) unarmed and ([+-]\d+(?:\.\d+)?%) (\w+) damage$', r'\1\2 damage while unarmed; \3 \4 damage'),
        (lab + r'([+-]\d+(?:\.\d+)?%) (?:more )?unarmed/(\w+) damage$', r'\1\2 damage while unarmed; \2 \3 damage'),
        (lab + r'unarmed and (\w+) hits ([+-]\d+(?:\.\d+)?%) damage$', r'\1\3 damage while unarmed; \3 \2 damage'),
        (lab + r'breathe underwater, swim (\d+(?:\.\d+)?)% faster$', r'\1water breathing; +\2% swim speed'),
        (lab + r'no mining slowdown underwater$', r'\1+0.8 underwater mining speed'),
        (lab + r'([+-]\d+(?:\.\d+)?)% underwater damage$', r'\1\2% damage underwater'),
        (lab + r'mobs (\d+(?:\.\d+)?)% slower to notice you$', r'\1-\2% detection range'),
        (lab + r'(?:you )?(?:can\'t|cannot) receive the glowing effect$', r'\1immune to glowing'),
        (lab + r'(?:no gear is worse than neutral for you|weapons you\'re clumsy or forbidden with count as neutral)$', r'\1+100% affinity floor'),
        (lab + r'heavy armor gives \+(\d+) armor per piece$', r'\1+\2 armor per heavy armor piece'),
        (r'\bfor (?:every|each) heavy armor piece(?: worn)?\b', r'per heavy armor piece'),
        (r'per (\d+)% missing health\b', r'per \1 missing health percent'),
        (r'per missing heart\b', r'per missing heart'),
        (lab + r'(?:all your stats|every stat|all stats) \+(\d+)(?: \(.*\))?$', r'\1+\2 to every stat'),
    ]
    for pat, rep in subs:
        text = re.sub(pat, rep, text, flags=re.I)
    return text


def translate(text, ctx):
    effects, unsupported = _translate_line(text, ctx)
    # A reaction defined on a labelled line is named after the label, so upgrades can find it
    # ("Guarded Casting: whenever you spend Ward, gain +3 armor" <- "Guarded Casting lasts 5s longer").
    m = named.LABEL.match((text or '').strip())
    single = effects[0] if len(effects) == 1 else None
    if single and single.get('type') == 'pipican:conditional' and len(single.get('effects', [])) == 1:
        single = single['effects'][0]
    if m and single and single.get('type') == 'pipican:on' and 'name' not in single and not unsupported:
        single['name'] = m.group(1).strip().lower()
    # Remember every reaction name the class has, for skill triggers ("landing Trickery hits").
    if ctx.get('class_id'):
        def walk(o):
            if isinstance(o, dict):
                if o.get('type') == 'pipican:on' and o.get('name'):
                    named.REACTION_NAMES.setdefault(ctx['class_id'], set()).add(o['name'])
                    if any(a.get('type') == 'pipican:empower' for a in o.get('actions', [])):
                        named.EMPOWER_REACTIONS.setdefault(ctx['class_id'], set()).add(o['name'])
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
        walk(effects)
    return effects, unsupported


def _translate_line(text, ctx):
    if not text or text.strip() in ('-', 'none', 'nothing'):
        return [], []
    original = text
    whole = named.whole_line(text, ctx)
    if whole is not None:
        return whole, []
    # Zeal's "(+10 Madness)" is part of what happens, not a note.
    text = re.sub(r'shrug it off entirely \(\+(\d+) ([A-Za-z ]+)\)', r'shrug it off entirely and gain \1 \2', text)
    # Class items: "Sigil weapons: +10% damage", "+3% bound weapon damage", "staff damage +1% per Staffcraft",
    # "sigil armor: +1 armor per piece", the bound weapon's definition.
    lab = r"^((?:[A-Z][\w' -]{1,30}:\s+)?)"
    text = re.sub(r'a book, writable book or enchanted book is in your off-hand', 'a book is in your off-hand', text, flags=re.I)
    text = re.sub(lab + r'a blaze rod or stick held in the main hand counts as your staff - (.+)$', r'\1\2 with your staff', text)
    if re.search(r'the weapon in hotbar slot 1 is your bound weapon', text, re.I):
        text = re.sub(lab + r'the weapon in hotbar slot 1 is your bound weapon:\s*', r'\1', text, flags=re.I)
        text = re.sub(r',\s*(?:and\s+)?it\b', '; it', text)
        text = re.sub(r'; it loses durability', '; bound weapon loses durability', text)
    text = re.sub(r'\bwith it\b', 'with your bound weapon', text) if re.search(r'bound weapon', original, re.I) else text
    # favored weapon: "+5% favored damage", "Favored damage +0.6% per Mastery", "+1 armor with favored weapon",
    # "+10% favored crit damage"
    text = re.sub(r'\bfavou?red crit(?:ical)? damage\b', 'favored critical damage', text, flags=re.I)
    text = re.sub(r'\bwith (?:your )?favou?red weapon\b', 'with your favored weapon', text, flags=re.I)
    item = r'(bound weapon|sigil(?:-marked)? weapons?|staff|favou?red(?: weapon)?)'
    text = re.sub(rf'^{item}:\s*([+-]\d+(?:\.\d+)?% (?:more )?(?:damage|critical damage))(.*)$', r'\2 with your \1\3', text, flags=re.I)
    text = re.sub(lab + r'while (.+?), sigil weapons (?:glow and )?deal \+(\d+)% more$', r'\1while \2: +\3% damage with your sigil weapon', text, flags=re.I)
    text = re.sub(lab + rf'([+-]\d+(?:\.\d+)?%) {item} (damage|critical damage)\b(.*)$', r'\1\2 \4 with your \3\5', text, flags=re.I)
    text = re.sub(lab + rf'{item} (damage|critical damage) ([+-]\d+(?:\.\d+)?%)(.*)$', r'\1\4 \3 with your \2\5', text, flags=re.I)
    text = re.sub(r'\bwith your (?:favored|favoured)(?: weapon)?\b', 'while holding your favored weapon', text, flags=re.I)
    text = re.sub(r'(while holding your favored weapon) (per .+)$', r'\2 \1', text, flags=re.I)
    text = re.sub(r'(^|;\s*)sigil armor:\s*(.+?) per piece(?=;|$)', r'\1\2 per sigil piece', text, flags=re.I)
    # Phrasings: "you take +10% damage while moving", "spears deal +15% damage while mounted", "immune to X always",
    # "No fall damage below 8 blocks", "+20% stealth", "mobs notice you from 20% farther away".
    text = re.sub(lab + r'you take ([+-]\d+(?:\.\d+)?%) (?:more )?damage\b', r'\1\2 damage taken', text, flags=re.I)
    text = re.sub(lab + r'(swords|axes|spears|maces|tridents|bows|crossbows) deal ([+-]\d+(?:\.\d+)?%) (?:more )?damage\b(?! to you)', r'\1\3 damage with \2', text, flags=re.I)
    text = re.sub(r'(immune to .+?) always$', r'\1', text, flags=re.I)
    text = re.sub(lab + r'\+(\d+(?:\.\d+)?)% stealth(?: \(smaller mob detection range\))?', r'\1-\2% detection range', text, flags=re.I)
    text = re.sub(r'(?:hostile )?mobs (?:notice|detect) you (?:from )?(\d+(?:\.\d+)?)% (farther|further)(?: away)?', r'+\1% detection range', text, flags=re.I)
    text = re.sub(r'(?:hostile )?mobs (?:notice|detect) you (\d+(?:\.\d+)?)% later', r'-\1% detection range', text, flags=re.I)
    # "+5% unarmed damage" -> "+5% damage while unarmed"
    text = re.sub(lab + r'([+-]\d+(?:\.\d+)?%) unarmed (damage|attack speed)\b', r'\1\2 \3 while unarmed', text, flags=re.I)
    # "your fire damage to mobs is +15%", "+20% weapon and magic damage"
    text = re.sub(lab + r'your (\w+ )?damage(?: to mobs)? is ([+-]\d+(?:\.\d+)?%)$', r'\1\3 \2damage', text, flags=re.I)
    text = re.sub(lab + r'([+-]\d+(?:\.\d+)?%) (weapon|melee|ranged|magic|fire) and (weapon|melee|ranged|magic|fire) damage\b', r'\1\2 \3 damage, \2 \4 damage', text, flags=re.I)
    # "+15% bow and crossbow damage", "+10% unarmed and +10% axe damage", "unarmed and pickaxe hits +20% damage"
    wpn = r'(bow|crossbow|axe|mace|sword|spear|trident|unarmed|pickaxe|dagger)'
    text = re.sub(lab + rf'([+-]\d+(?:\.\d+)?%) {wpn} and {wpn} damage\b', r'\1\2 \3 damage; \2 \4 damage', text, flags=re.I)
    text = re.sub(lab + rf'([+-]\d+(?:\.\d+)?%) {wpn} and ([+-]\d+(?:\.\d+)?%) {wpn} damage\b', r'\1\2 \3 damage; \4 \5 damage', text, flags=re.I)
    text = re.sub(lab + rf'{wpn} and {wpn} hits ([+-]\d+(?:\.\d+)?%) damage\b', r'\1\4 \2 damage; \4 \3 damage', text, flags=re.I)
    text = re.sub(r'([+-]\d+(?:\.\d+)?%) unarmed damage\b', r'\1 damage while unarmed', text, flags=re.I)
    # "your max health -10%", "you take 10% more (fire) damage", "you take 25% more damage from X"
    text = re.sub(r'\byour max health ([+-]\d+(?:\.\d+)?%)', r'\1 max health', text, flags=re.I)
    text = re.sub(r'([+-]\d+(?:\.\d+)?%) regeneration\b(?! (?:i|ii|potions?))', r'\1 natural regeneration', text, flags=re.I)
    text = re.sub(r'\byou take (\d+(?:\.\d+)?)% more damage from\b', r'take \1% more damage from', text, flags=re.I)
    text = re.sub(r'\byou take (\d+(?:\.\d+)?)% more ((?:fire|magic|fall|freezing|explosion|projectile) )?damage\b(?! from)', r'+\1% \2damage taken', text, flags=re.I)
    # "in human form you deal -20% damage" -> "-20% damage in human form"
    text = re.sub(r'\bin (human|any animal|animal|any) form,? you deal ([+-]\d+(?:\.\d+)?%) (?:more )?damage\b', r'\2 damage in \1 form', text, flags=re.I)
    text = _bulk(text)
    # jump height in blocks -> jump strength (about +0.15 per block)
    text = re.sub(lab + r'(?:jump height \+(\d+(?:\.\d+)?) blocks?|\+(\d+(?:\.\d+)?) (?:extra )?jump (?:block )?height)\b',
                  lambda m: f'{m.group(1)}+{float(m.group(2) or m.group(3)) * 0.15:g} jump strength', text, flags=re.I)
    text = re.sub(lab + r'\+(\d+(?:\.\d+)?) jump height\b', lambda m: f'{m.group(1)}+{float(m.group(2)) * 0.15:g} jump strength', text, flags=re.I)
    # "-1 armor and -4% movement speed per heavy armor piece": the trailing "per X" covers both parts.
    m = re.match(r'^((?:[A-Z][\w\' -]{1,30}:\s+)?)([+-]\d+(?:\.\d+)?%? [a-z][a-z ]*?) and ([+-]\d+(?:\.\d+)?%? [a-z][a-z ]*?) (per (?:\d+ )?[a-z][\w \'-]*)$', text.strip())
    if m and 'per' not in m.group(2):
        text = f'{m.group(1)}{m.group(2)} {m.group(4)}, {m.group(3)} {m.group(4)}'
    # Temple upgrades: "+5% temple damage", "+1 more temple armor", "Per 10 Sanctum, temple armor bonus +1"
    text = re.sub(lab + r'([+-]\d+(?:\.\d+)?%?) (?:more )?temple (damage|armor)\b', r'\1\2 \3 inside your temple', text, flags=re.I)
    text = re.sub(lab + r'(per \d+ \w+,\s*)?temple (armor|damage) bonus ([+-]\d+(?:\.\d+)?%?)', r'\1\2\4 \3 inside your temple', text, flags=re.I)
    # Healing received: "healing from Regeneration/Instant Health is 25% weaker", "Regeneration and Instant Health are 50% weaker on you",
    # "healing you receive is +15%".
    text = re.sub(r'(?:healing from )?regeneration(?:/|, | and )instant health(?:,? and healing from other players)? (?:is|are) (\d+)% weaker(?: on you)?',
                  r'-\1% healing received', text, flags=re.I)
    text = re.sub(r'healing you receive is ([+-]\d+)%', r'\1% healing received', text, flags=re.I)
    text = re.sub(r'healing on you is (\d+)% (weaker|stronger)', lambda m: ('-' if m.group(2) == 'weaker' else '+') + m.group(1) + '% healing received', text, flags=re.I)
    text = re.sub(r'(?:instant health and regeneration|regeneration and instant health) potions you throw or drink are (\d+)% stronger',
                  r'+\1% healing received', text, flags=re.I)
    # Resource regen: "Verdance regen doubles in forests; it stops completely in the Nether and the End", "Voltage regen stops while wet",
    # "-10% Embers drain in lava", "+1 Devotion regen per 10s".
    m = re.search(r'([A-Z][\w]+(?: [A-Z][\w]+)?) regen doubles', text)
    if m:
        res = m.group(1)
        text = re.sub(rf'{res} regen doubles (in .+?)(;|$)', rf'+100% {res} regen \1\2', text)
        text = re.sub(r'it stops completely (in .+)$', rf'no {res} regen \1', text)
    text = re.sub(r'([A-Z][\w]+(?: [A-Z][\w]+)?) regen stops (while .+?|in .+?)(?= and |;|$)', r'no \1 regen \2', text)
    text = re.sub(r'([+-]\d+(?:\.\d+)?)% ([A-Z][\w]+(?: [A-Z][\w]+)?) drain\b', lambda m: f'{-float(m.group(1)):+g}% {m.group(2)} regen', text)
    text = re.sub(r'\+(\d+(?:\.\d+)?) ([A-Z][\w]+(?: [A-Z][\w]+)?) regen per (\d+)s$', lambda m: f'+{float(m.group(1)) / float(m.group(3)):g} {m.group(2)} regen', text)
    # "hunger drains 20% slower", "critical hits deal +20% damage", "+20% attack and movement speed"
    text = re.sub(r'hunger drains (\d+(?:\.\d+)?)% (slower|faster)', lambda m: f'{"-" if m.group(2) == "slower" else "+"}{m.group(1)}% hunger', text, flags=re.I)
    text = re.sub(r'critical hits deal \+(\d+(?:\.\d+)?)% damage', r'+\1% critical damage', text, flags=re.I)
    text = re.sub(r'([+-]\d+(?:\.\d+)?%) (attack|movement|swim|mining) and (attack|movement|swim|mining) speed', r'\1 \2 speed and \1 \3 speed', text, flags=re.I)
    # "(including you)" decides who an aura reaches; it isn't a note.
    text = re.sub(r'\s*\(including you\)', ' including you', text, flags=re.I)
    # "+10% damage and immunity to Darkness, Nausea and Blindness": keep the immunity list together.
    if not re.search(r'players within', text, re.I):
        text = re.sub(r',?\s+and (immun(?:e|ity) to [^;,]+(?:, [^;,]+)*(?:,? and [^;,]+)?)$', r'; \1', text)
    # "(60s cooldown)" limits how often a reaction fires; keep it.
    text = re.sub(r'\s*\((\d+(?:\.\d+)?)s cooldown\)', r' with a \1s cooldown', text, flags=re.I)
    # Parenthetical notes are explanations ("(potions, evoker fangs...)"); they only break the comma split.
    text = re.sub(r'\s*\([^)]*\)', '', text)
    # "after standing still for 3s, +20% damage until you move": the still condition already ends when you move.
    if re.search(r'standing still|not moving', text, re.I):
        text = re.sub(r'\s+until you move\b', '', text, flags=re.I)
    # "Stand Guard: after standing still for 3s, +20% damage": the label only names the leading condition's bonus.
    text = re.sub(r"^[A-Z][\w' -]{1,30}:\s+(?=(?:after|while|whenever|when|during|if)\b[^,:]*,)", '', text.strip())
    # "Per 10 Brawn, +1 max health" -> "+1 max health per 10 Brawn"
    m = re.match(r"^\s*((?:[A-Z][\w' -]{1,30}:\s+)?)per (\d+ )?([A-Za-z][\w \'-]*?)(?: points?)?,\s*(.+)$", text.strip(), re.I)
    # (Chance lines keep their leading "per": the scaled-reaction rule reads them that way.)
    if m and ',' not in m.group(4) and ';' not in m.group(4) and not (m.group(1) and re.search(r'\bchance (?:your|on|that your|to|for your)\b', m.group(4), re.I)):
        text = f'{m.group(1)}{m.group(4)} per {m.group(2) or ""}{m.group(3)}'
    text = re.sub(r'^((?:[A-Z][\w\' -]{1,30}:\s+)?)(?:jump height \+(\d+(?:\.\d+)?) blocks?|\+(\d+(?:\.\d+)?) (?:extra )?jump (?:block )?height)\b',
                  lambda m: f'{m.group(1)}+{float(m.group(2) or m.group(3)) * 0.15:g} jump strength', text, flags=re.I)
    # "while Rage is above 75%: A, B, but C": a leading condition with a colon covers everything after it.
    m = re.match(r'^(?:[A-Z][\w\' -]{1,30}:\s+)?((?:while|when|whenever|as long as)\s+[^:]+?):\s+(.+)$', text.strip())
    if m:
        when = cond.parse(m.group(1), ctx)
        if when is not None:
            inner, uns = translate(m.group(2), ctx)
            if uns or not inner:
                return [], [original.strip()]
            return [{'type': 'pipican:conditional', 'when': when, 'effects': inner}], []
    # "+1 armor while standing on stone per 10 Bedrock" / "... piece, per 20 Ingenuity": scaling goes before the condition.
    m = re.match(r'^(.+?)\s+((?:while|when|in|inside|on(?! you\b)|at|with)\s.+?),?\s+(per \d+ [A-Za-z][\w\' -]*?(?: points?)?)$', text.strip())
    if m and ';' not in text and not re.search(r'\bbut\b', text):
        text = f'{m.group(1)} {m.group(3)} {m.group(2)}'
    # Segments ("A in water, but B on dry land"; "A while wet; B") each get their own condition.
    effects, unsupported = [], []
    for seg in re.split(r';\s*|,?\s+but\s+|,\s+and\s+(?=[+-]?\d|takes?\b|no\b|when\b|whenever\b|after\b|every\b|a lightning\b|(?:swords|axes|maces|spears|tridents|bows|crossbows) deal\b|villagers\b)', text):
        if seg.strip():
            e, u = _segment(seg.strip(), ctx)
            effects += e
            unsupported += u
    # "upside, but downside" and keystones: never hand out only one half.
    # A note in brackets can qualify everything ("Divine Ecstasy (toggle, drains 3/s): +10% damage").
    notes = ' '.join(re.findall(r'\(([^)]*)\)', original))
    if unsupported and effects and (re.search(r'\bbut\b', original, re.I) or ctx.get('keystone') or re.search(r'\b(?:toggle|drains?|cooldown|while|when|only|active|lasts?)\b', notes, re.I)):
        return [], [original.strip()]
    return effects, unsupported


def _segment(seg, ctx):
    seg = _bulk(seg.strip())
    mounted = bonus.mount_subject(_clean(seg).lower())
    if mounted == 'unsupported':
        return [], [seg.strip()]
    if mounted is not None:
        return mounted, []
    reacted = combat.reaction(_clean(seg).lower()) or bonus.resource(_clean(seg).lower(), ctx) or bonus.boost(_clean(seg).lower(), ctx) or bonus.scaled_reaction(_clean(seg).lower(), ctx) or named.segment(_clean(seg).lower(), ctx)
    if reacted is not None:
        return reacted, []
    wrapped = _conditional(seg, ctx)
    if wrapped is not None:
        return wrapped, []
    effects, unsupported = _translate(seg, ctx)
    # A partial translation is only safe when the parts that failed are separate extras. If the first part failed
    # it usually names who gets the bonus ("Tamed wolves get +10% damage"), so nothing may apply. A qualifier
    # in a failed part ("in Ecstasy", "for every heavy piece", a condition word) usually covers the whole segment.
    first_failed = bool(unsupported) and _clean(_split(seg)[0]) in unsupported
    qualified = re.search(r'\bin [A-Z]', seg) or CONDITION.search(seg) or \
        any(re.search(r'\b(?:for every|for each|per)\b', u, re.I) for u in unsupported)
    if unsupported and effects and (first_failed or qualified):
        return [], [seg]
    if unsupported and not effects:
        # Whole-line phrases ("killing a marked target heals you 25%") also work as one segment of a longer line.
        whole = named.whole_line(seg, ctx)
        if whole is not None:
            return whole, []
    return effects, unsupported


def _conditional(text, ctx):
    """"while X: effects" / "effects while X" / "effects underground" -> [pipican:conditional], or None."""
    t = text.strip().rstrip('.')
    t = re.sub(r'^[A-Z][\w\' -]{1,30}:\s+(?=(?:while|when|whenever|as long as|if|below|above|in|at|on|mounted)\b)', '', t)
    # "mounted damage +25%" / "+5% mounted damage" -> "+25% damage while mounted"
    t = re.sub(r'^mounted (damage|attack speed) ([+-]\d+(?:\.\d+)?%)( per .+)?$', lambda m: f'{m.group(2)} {m.group(1)}{m.group(3) or ""} while mounted', t, flags=re.I)
    t = re.sub(r'^([+-]\d+(?:\.\d+)?%) mounted (damage|attack speed)$', r'\1 \2 while mounted', t, flags=re.I)
    m = re.match(r'^(?:while|when|whenever|as long as|if)\s+(.+?)(?::|,)\s+(.+)$', t, re.I)
    # "below half health, +20% melee damage": a condition first, without "while"
    m2 = re.match(r'^([^,:]+?)(?::|,)\s+([+-]?\d.*)$', t)
    if m:
        cond_text, eff_text = m.group(1), m.group(2)
    elif m2 and cond.parse(m2.group(1), ctx) is not None:
        cond_text, eff_text = m2.group(1), m2.group(2)
    else:
        m = cond.TRAILING.search(t)
        if not m or m.start() == 0:
            # Named states of the class: "+15% knockback resistance in Ecstasy".
            m = next((x for x in re.finditer(r'\s+(?=(?:while|in|during)\s)', t) if x.start() > 0
                      and cond.parse(t[x.start():], ctx) is not None), None)
            if m is None:
                return None
        eff_text, cond_text = t[:m.start()], t[m.start():]
    when = cond.parse(cond_text, ctx)
    if when is None:
        return None
    # "-2 armor at all times and +10% damage taken while sprinting": only the rest is conditional.
    # A resource bar's max isn't a situational bonus ("+0.5 Ecstasy max and +0.2% damage while in Divine Ecstasy").
    rn = re.escape((ctx.get('resource_name') or '\0').lower())
    keep = re.compile(rf'\bat all times\b|\balways\b|^[+-]?\d+(?:\.\d+)?%? (?:max {rn}|{rn} max)\b', re.I)
    always_text = ', '.join(c for c in _split(eff_text) if keep.search(c.strip()))
    eff_text = ', '.join(c for c in _split(eff_text) if not keep.search(c.strip()))
    always, unsupported_always = _translate(always_text, ctx) if always_text else ([], [])
    effects, unsupported = _translate(eff_text, ctx) if eff_text else ([], [])
    if unsupported or unsupported_always or not effects:
        return None
    return always + [{'type': 'pipican:conditional', 'when': when, 'effects': effects}]


def _translate(text, ctx):
    effects, unsupported = [], []
    clauses = _split(text)
    i = 0
    while i < len(clauses):
        cl = _clean(clauses[i])
        # "+5% weapon damage, +1% more every 10 class levels": merge the scaling tail into the previous effect.
        m = re.match(r'^\s*\+?(\d+(?:\.\d+)?)%?\s+more\s+(?:every|per)\s+(\d+)\s+(class levels?|levels?)\s*$', cl.strip(), re.I)
        if m and effects and 'multiplier' in effects[-1] or (m and effects and effects[-1].get('type') == 'pipican:attribute'):
            per = float(m.group(1)) / float(m.group(2))
            last = effects[-1]
            if 'multiplier' in last:
                last.setdefault('scaling', []).append({'source': 'class_level', 'per': round(per / 100, 6)})
            else:
                op = last.get('operation', 'add_value')
                last.setdefault('scaling', []).append({'source': 'class_level', 'per': round(per / 100 if op != 'add_value' else per, 6)})
            i += 1
            continue
        res = _one(cl, ctx)
        if res is None:
            unsupported.append(_clean(cl))
        else:
            effects.extend(res)
        i += 1
    return effects, unsupported

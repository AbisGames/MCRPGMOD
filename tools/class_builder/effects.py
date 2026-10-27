"""Translate plain-language bonus phrases into pipican effect JSON.

translate(text, ctx) -> (effects, unsupported_clauses)
ctx: {'special_name': 'Grit', 'special_id': 'pipican:warrior_grit', 'implicit_per_point': bool}
"""
import re

import bonus
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
    'ranged': ['#pipican:weapons/ranged'],
}
# Damage-type tags. pipican:* ones are shipped by this build (see DAMAGE_TYPE_TAGS).
DAMAGE_TAGS = {
    'fire': '#minecraft:is_fire', 'fall': '#minecraft:is_fall', 'projectile': '#minecraft:is_projectile',
    'explosion': '#minecraft:is_explosion', 'explosive': '#minecraft:is_explosion', 'freezing': '#minecraft:is_freezing',
    'drowning': '#minecraft:is_drowning', 'lightning': '#minecraft:is_lightning',
    'magic': '#pipican:is_magic', 'wither': '#pipican:is_wither', 'melee': '#pipican:is_melee', 'poison': '#pipican:is_magic',
    'ranged': '#minecraft:is_projectile',
}
DAMAGE_TYPE_TAGS = {
    'is_magic': ['minecraft:magic', 'minecraft:indirect_magic', 'minecraft:sonic_boom', 'minecraft:thorns'],
    'is_wither': ['minecraft:wither', 'minecraft:wither_skull'],
    'is_melee': ['minecraft:player_attack', 'minecraft:mob_attack', 'minecraft:mob_attack_no_aggro'],
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
    'jump strength': ('minecraft:jump_strength', 'pct_base'),
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
    else:
        return None, rest
    return (source, every), rest[:m.start()].strip()


def _one(clause, ctx):
    """Translate one clause. Returns list of effects or None if unsupported."""
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
    r = re.escape(rn)
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
    for special in (combat.immune(low), combat.taken_from(low), combat.critical(low, ctx, scal)):
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


def translate(text, ctx):
    if not text or text.strip() in ('-', 'none', 'nothing'):
        return [], []
    original = text
    # Parenthetical notes are explanations ("(potions, evoker fangs...)"); they only break the comma split.
    text = re.sub(r'\s*\([^)]*\)', '', text)
    # "Per 10 Brawn, +1 max health" -> "+1 max health per 10 Brawn"
    m = re.match(r'^\s*per (\d+ )?([A-Za-z][\w \'-]*?)(?: points?)?,\s*(.+)$', text.strip(), re.I)
    if m and ',' not in m.group(3) and ';' not in m.group(3):
        text = f'{m.group(3)} per {m.group(1) or ""}{m.group(2)}'
    # Segments ("A in water, but B on dry land"; "A while wet; B") each get their own condition.
    effects, unsupported = [], []
    for seg in re.split(r';\s*|,?\s+but\s+|,\s+and\s+(?=[+-]?\d|takes?\b|no\b)', text):
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
    reacted = combat.reaction(_clean(seg).lower())
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
    return effects, unsupported


def _conditional(text, ctx):
    """"while X: effects" / "effects while X" / "effects underground" -> [pipican:conditional], or None."""
    t = text.strip().rstrip('.')
    t = re.sub(r'^[A-Z][\w\' -]{1,30}:\s+(?=(?:while|when|whenever|as long as|if|below|above|in|at|on|mounted)\b)', '', t)
    # "mounted damage +25%" / "+5% mounted damage" -> "+25% damage while mounted"
    t = re.sub(r'^mounted (damage|attack speed) ([+-]\d+(?:\.\d+)?%)$', r'\2 \1 while mounted', t, flags=re.I)
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
            return None
        eff_text, cond_text = t[:m.start()], t[m.start():]
    when = cond.parse(cond_text, ctx)
    if when is None:
        return None
    # "-2 armor at all times and +10% damage taken while sprinting": only the rest is conditional.
    always_text = ', '.join(c for c in _split(eff_text) if re.search(r'\bat all times\b|\balways\b', c))
    eff_text = ', '.join(c for c in _split(eff_text) if not re.search(r'\bat all times\b|\balways\b', c))
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

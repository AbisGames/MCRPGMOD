"""pipican:bonus kinds (XP, potion durations, hunger, trade prices, draw/reload speed) and per-weapon attack speed."""
import re

N = r'(\d+(?:\.\d+)?)'

# "+10% <what>" (number first)
LEADING = [
    (r'(?:experience|xp)(?: gained| gain| from all sources)?', 'experience'),
    (r'(?:potion|potion effect) duration|duration of potions(?: you drink)?|potion effect length', 'potion_duration'),
    (r'trade prices?|villager prices', 'trade_price'),
    (r'(?:crossbow )?reload(?:ing)? speed', 'reload_speed'),
    (r'(?:bow )?draw speed', 'draw_speed'),
    (r'(?:natural )?(?:health )?regeneration|natural regen', 'natural_regen'),
    (r'hunger(?: drain)?|hunger loss|food drain', 'hunger'),
    (r'(?:mob )?detection range|detection range of mobs', 'detection_range'),
    (r'skill training speed|skill training|mastery gain|skill xp', 'skill_training'),
]

# "<potions...> last 20% longer/shorter"
POTIONS = [
    (r'(?:your |the )?(?:potions|potion effects)(?: you drink| on you)?', 'potion_duration'),
    (r'(?:your )?(?:harmful potions|harmful potion effects|potion debuffs|debuffs)(?: on you)?', 'harmful_duration'),
    (r'(?:beneficial|good) (?:potions|potion effects)(?: on you)?', 'beneficial_duration'),
    (r'harmful (?:potion effects|effects) you apply|harmful (?:splash )?potions you throw', 'applied_harmful_duration'),
    (r'(?:potion effects|effects) you apply|thrown splash(?:/lingering)? potions', 'applied_duration'),
]

WEAPON_SPEED = {'sword': '#pipican:weapons/swords', 'axe': '#pipican:weapons/axes', 'mace': '#pipican:weapons/maces',
                'spear': '#pipican:weapons/spears', 'trident': '#pipican:weapons/tridents'}


MOUNT_ATTRS = {'speed': 'minecraft:movement_speed', 'movement speed': 'minecraft:movement_speed', 'jump': 'minecraft:jump_strength',
               'jump strength': 'minecraft:jump_strength', 'jump height': 'minecraft:jump_strength', 'max health': 'minecraft:max_health',
               'health': 'minecraft:max_health', 'armor': 'minecraft:armor', 'armour': 'minecraft:armor'}


def _mount_or_attr(attr, amount, op, s, mount=True):
    e = {'type': 'pipican:mount' if mount else 'pipican:attribute', 'attribute': attr, 'amount': round(amount, 6)}
    if op != 'add_value':
        e['operation'] = op
    if s:
        e['scaling'] = [{'source': src, 'per': round(per / (100 if op != 'add_value' else 1), 6)} for src, per in s]
    return e


def mount_flat(low, scal):
    """Flat mount bonuses: "+2 mount max health", "mount +2 armor", "mount armor +1" (before "per 10 Horsemanship")."""
    m = re.fullmatch(rf'\+?{N} mount (max health|health|armor|armour)|(?:your )?mount (?:gets )?\+{N} (max health|health|armor|armour)|mount (max health|armor|armour) \+{N}', low)
    if not m:
        return None
    num = m.group(1) or m.group(3) or m.group(6)
    what = m.group(2) or m.group(4) or m.group(5)
    v, s = scal(float(num))
    return [_mount_or_attr(MOUNT_ATTRS[what], v, 'add_value', s)]


def _effect(kind, v, s):
    e = {'type': 'pipican:bonus', 'kind': kind, 'amount': round(v / 100, 6)}
    if s:
        e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
    return e


def leading(value, what, scal):
    """Number-led clause: value is the signed number, what the text after "%"."""
    for pat, kind in LEADING:
        if re.fullmatch(pat, what):
            v, s = scal(value)
            return [_effect(kind, v, s)]
    m = re.fullmatch(r'(sprint speed|hunger from sprinting|sprinting hunger)', what)
    if m:
        v, s = scal(value)
        inner = _effect('hunger', v, s) if 'hunger' in what else _mount_or_attr('minecraft:movement_speed', v / 100, 'add_multiplied_base', s, mount=False)
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sprinting'}, 'effects': [inner]}]
    m = re.fullmatch(r'mount (speed|movement speed|jump|jump strength|jump height|max health|health|armor)', what)
    if m:
        v, s = scal(value)
        attr = MOUNT_ATTRS[m.group(1)]
        return [_mount_or_attr(attr, v / 100, 'add_multiplied_base', s)]
    m = re.fullmatch(r'(sword|axe|mace|spear|trident) attack speed', what)
    if m:
        v, s = scal(value)
        attr = {'type': 'pipican:attribute', 'attribute': 'minecraft:attack_speed', 'amount': round(v / 100, 6), 'operation': 'add_multiplied_base'}
        if s:
            attr['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': WEAPON_SPEED[m.group(1)]}, 'effects': [attr]}]
    return None


def special(low, scal):
    """Clauses that don't start with the number."""
    mf = mount_flat(low, scal)
    if mf:
        return mf
    m = re.fullmatch(rf'(?:your )?skills train {N}% (faster|slower)', low)
    if m:
        v, s = scal(float(m.group(1)) * (1 if m.group(2) == 'faster' else -1))
        return [_effect('skill_training', v, s)]
    m = re.fullmatch(r'immune to (lightning|fire|fall|drowning|freezing|explosion|explosive)(?: damage)?', low)
    if m:
        import effects as fx
        tag = fx.DAMAGE_TAGS.get({'explosive': 'explosion'}.get(m.group(1), m.group(1)))
        if tag:
            return [{'type': 'pipican:damage_taken', 'damage_types': tag, 'multiplier': 0.0}]
    for pat, kind in POTIONS:
        m = re.fullmatch(rf'(?:{pat}) last {N}% (longer|shorter)', low)
        if m:
            v, s = scal(float(m.group(1)) * (1 if m.group(2) == 'longer' else -1))
            return [_effect(kind, v, s)]
    m = re.fullmatch(rf'(?:you )?(?:lose|get) hunger {N}% (slower|faster)|(?:you )?get hungry {N}% (slower|faster)', low)
    if m:
        num, way = (m.group(1), m.group(2)) if m.group(1) else (m.group(3), m.group(4))
        v, s = scal(float(num) * (1 if way == 'faster' else -1))
        return [_effect('hunger', v, s)]
    m = re.fullmatch(rf'(reloading|drawing a bow|bow draw) is {N}% faster', low)
    if m:
        v, s = scal(float(m.group(2)))
        return [_effect('reload_speed' if m.group(1) == 'reloading' else 'draw_speed', v, s)]
    # "potion duration +0.5%" (after "per Flux" was split off as scaling)
    m = re.fullmatch(rf'(.+?) \+?([+-]?{N[1:-1]})%', low)
    if m:
        try:
            return leading(float(m.group(2)), m.group(1).strip(), scal)
        except ValueError:
            return None
    return None


# ---------------------------------------------------------------- resource bar filling / spending

def _on(event, actions, **extra):
    return dict({'type': 'pipican:on', 'event': event, 'actions': actions}, **extra)


def _fill(amount=None, percent=None):
    a = {'type': 'pipican:resource'}
    if amount is not None:
        a['amount'] = float(amount)
    if percent is not None:
        a['percent'] = float(percent)
    return a


HITS = {'': 'hit', 'melee ': 'melee_hit', 'ranged ': 'ranged_hit', 'arrow ': 'ranged_hit'}


def _spend_outcome(t):
    """What spending does: "gain Speed II for 3s", "heal 2 HP", "mobs within 8 blocks get Weakness I for 4s"."""
    import combat
    t = t.strip().rstrip('.')
    m = re.fullmatch(r'(?:you )?heals? (?:you )?(?:for )?(\d+(?:\.\d+)?) ?(?:hp|health)', t)
    if m:
        return [{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(1))}]
    m = re.fullmatch(r'(?:all )?(?:hostile )?mobs within (\d+) blocks (?:get|are given|receive) (.+)', t)
    if m:
        inner = combat.outcome('give ' + m.group(2), 'victim')
        if inner and len(inner) == 1 and inner[0]['type'] == 'pipican:effect':
            return [dict(inner[0], target='hostiles', radius=float(m.group(1)))]
        return None
    m = re.fullmatch(r'a shockwave hits (?:all )?mobs within (\d+) blocks for (\d+(?:\.\d+)?) damage', t)
    if m:
        return [{'type': 'pipican:damage', 'target': 'hostiles', 'radius': float(m.group(1)), 'amount': float(m.group(2))}]
    out = combat.outcome(t, 'self')
    if out and all(a['type'] == 'pipican:effect' and a.get('target') == 'self' for a in out):
        return out
    return None


def resource(low, ctx):
    """Whole-segment resource phrases: filling from hits/kills/damage/sleep, spending triggers, fill speed, ability cost."""
    rn = (ctx.get('resource_name') or '').lower()
    t = low.strip().rstrip('.')
    if rn:
        # "Mystic Flux" may be written as just "Flux".
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
        m = re.fullmatch(rf'(?:your )?(|melee |ranged |arrow )(?:hits|attacks) (?:refill|refills|restore|restores|give|gives you|add|adds) {N} {r}', t)
        if m:
            return [_on(HITS[m.group(1)], [_fill(m.group(2))])]
        m = re.fullmatch(rf'{r} (?:fills|refills) by {N} (?:every time you take damage|whenever you take damage|per hit taken|when you are hit)', t)
        if m:
            return [_on('hurt', [_fill(m.group(1))])]
        m = re.fullmatch(rf'(?:every|each) kill (?:refills|restores|gives you|gives) {N} {r}|killing a mob (?:refills|restores) {N} {r}', t)
        if m:
            return [_on('kill', [_fill(m.group(1) or m.group(2))])]
        m = re.fullmatch(rf'{r} (?:refills?|is refilled) (?:completely|fully) (?:when|whenever|after) you sleep', t)
        if m:
            return [_on('wake', [_fill(percent=1)])]
        m = re.fullmatch(rf'{r} fills {N}% (faster|slower)|{r} fills (twice|three times) as fast', t)
        if m:
            v = float(m.group(1)) * (1 if m.group(2) == 'faster' else -1) if m.group(1) else {'twice': 100, 'three times': 200}[m.group(3)]
            return [_effect('resource_gain', v, None)]
        m = re.fullmatch(rf'(?:whenever|when|every time|each time) you spend {r},? (.+)', t)
        if m:
            out = _spend_outcome(m.group(1))
            if out:
                return [_on('spend', out)]
        m = re.fullmatch(rf'abilities cost {N}% (less|more) {r}', t)
        if m:
            return [_effect('ability_cost', float(m.group(1)) * (-1 if m.group(2) == 'less' else 1), None)]
    m = re.fullmatch(rf'(?:villager )?trades cost {N}% (less|more)', t)
    if m:
        return [_effect('trade_price', float(m.group(1)) * (-1 if m.group(2) == 'less' else 1), None)]
    m = re.fullmatch(rf'natural (?:health )?regeneration is {N}% (faster|slower)', t)
    if m:
        return [_effect('natural_regen', float(m.group(1)) * (1 if m.group(2) == 'faster' else -1), None)]
    return None


def fill_rules(note):
    """The resource's "starts full" note: "fills by 2 per hit taken and 1 per hit dealt, drains 0.5/s out of combat"."""
    note = (note or '').lower()
    out = []
    for n, what in re.findall(rf'{N} per (hit taken|hit dealt|kill)', note):
        out.append(_on({'hit taken': 'hurt', 'hit dealt': 'hit', 'kill': 'kill'}[what], [_fill(n)]))
    return out, 'out of combat' in note


# ---------------------------------------------------------------- short boosts after something happens

AFTER = [
    (r'after (?:you take damage|taking damage|being hit|you are hit|you get hit)|when you take damage|whenever you take damage', 'hurt'),
    (r'after (?:hitting a mob|you hit a mob|hitting something|each hit|a hit)|when you hit a mob', 'hit'),
    (r'after (?:a kill|killing a mob|you kill a mob|each kill)|when you kill a mob', 'kill'),
    (r'after blocking(?: an attack| a hit)?(?: with a shield)?', 'block'),
]


def _boost(inner, seconds, ctx):
    import effects as fx
    eff, uns = fx.translate(inner, dict(ctx, implicit_per_point=False))
    if not eff or uns:
        return None
    return {'type': 'pipican:boost', 'seconds': float(seconds), 'effects': eff}


def boost(t, ctx):
    """"+15% movement speed for 2s after you take damage", "after you take damage, +15% speed for 2s",
    "whenever you spend Ward, gain +3 armor for 10s"."""
    for pat, event in AFTER:
        m = re.fullmatch(rf'(.+?) for {N}\s?s(?:econds?)? (?:{pat})', t) or re.fullmatch(rf'(?:{pat}),? (?:gain |get |you gain |you get )?(.+?) for {N}\s?s(?:econds?)?', t)
        if m:
            b = _boost(m.group(1), m.group(2), ctx)
            if b:
                return [_on(event, [b])]
    rn = (ctx.get('resource_name') or '').lower()
    if rn:
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
        m = re.fullmatch(rf'(?:whenever|when|every time|each time) you spend {r},? (?:gain |get |you gain |you get )?(.+?) for {N}\s?s(?:econds?)?', t)
        if m:
            b = _boost(m.group(1), m.group(2), ctx)
            if b:
                return [_on('spend', [b])]
    return None


def mount_subject(t):
    """"your mount gets +4 armor and +10% speed": every part goes to the mount, or nothing applies.
    Returns effects, 'unsupported', or None if the line isn't about the mount."""
    m = re.fullmatch(r'(?:your )?mount (?:gets|has|gains) (.+)', t)
    if not m:
        return None
    out = []
    for part in re.split(r',\s*(?:and\s+)?|\s+and\s+', m.group(1)):
        pm = re.fullmatch(rf'\+?{N}(%?) (speed|movement speed|jump|jump strength|jump height|max health|health|armor|armour)', part.strip())
        if not pm:
            return 'unsupported'
        pct = pm.group(2) == '%'
        if not pct and pm.group(3) not in ('max health', 'health', 'armor', 'armour'):
            return 'unsupported'
        out.append(_mount_or_attr(MOUNT_ATTRS[pm.group(3)], float(pm.group(1)) / (100 if pct else 1),
                                  'add_multiplied_base' if pct else 'add_value', None))
    return out
